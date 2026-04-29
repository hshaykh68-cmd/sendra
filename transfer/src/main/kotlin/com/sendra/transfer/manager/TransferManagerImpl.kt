package com.sendra.transfer.manager

import com.sendra.connection.manager.ConnectionManager
import com.sendra.connection.transport.Transport
nimport com.sendra.core.constants.SendraConstants
import com.sendra.core.dispatcher.DispatcherProvider
import com.sendra.core.extensions.throttleLatest
import com.sendra.core.result.Result
import com.sendra.domain.model.*
import com.sendra.domain.repository.FileRepository
import com.sendra.domain.repository.TransferRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransferManagerImpl @Inject constructor(
    private val fileRepository: FileRepository,
    private val transferRepository: TransferRepository,
    private val connectionManager: ConnectionManager,
    private val dispatcherProvider: DispatcherProvider,
    private val resumeStateManager: ResumeStateManager
) : TransferManager {
    
    private val activeSessions = ConcurrentHashMap<SessionId, ActiveSession>()
    private val _transferStates = MutableSharedFlow<TransferState>(
        extraBufferCapacity = 64,
        replay = 1
    )
    override val transferStates: SharedFlow<TransferState> = _transferStates.asSharedFlow()
    
    override suspend fun initiateTransfer(
        files: List<FileInfo>,
        targetDevice: Device,
        resumeFromInterrupted: Boolean
    ): Result<TransferSession> = withContext(dispatcherProvider.io) {
        try {
            val sessionId = generateSessionId()
            
            val session = TransferSession(
                id = sessionId,
                direction = TransferDirection.SEND,
                status = TransferStatus.PREPARING,
                targetDevice = targetDevice,
                files = files,
                totalBytes = files.sumOf { it.size }
            )
            
            transferRepository.createSession(session)
            
            // Establish connection
            val connectionResult = connectionManager.establishConnection(sessionId, targetDevice)
            if (connectionResult.isError) {
                transferRepository.updateSessionStatus(sessionId, TransferStatus.FAILED)
                return@withContext Result.Error(
                    connectionResult.exceptionOrNull() ?: IOException("Connection failed"),
                    "Could not connect to device"
                )
            }
            
            val transport = connectionResult.getOrNull()!!
            
            // Start transfer
            startTransferSession(session, transport)
            
            Result.Success(session)
        } catch (e: Exception) {
            Timber.e(e, "Failed to initiate transfer")
            Result.Error(e, "Transfer initiation failed: ${e.message}")
        }
    }
    
    override suspend fun acceptTransfer(
        sessionId: SessionId,
        files: List<FileInfo>,
        senderDevice: Device
    ): Result<TransferSession> = withContext(dispatcherProvider.io) {
        try {
            val session = TransferSession(
                id = sessionId,
                direction = TransferDirection.RECEIVE,
                status = TransferStatus.PREPARING,
                targetDevice = senderDevice,
                files = files,
                totalBytes = files.sumOf { it.size }
            )
            
            transferRepository.createSession(session)
            
            // For receiving, we wait for connection from sender
            // Connection is established via the accept flow in ReceiveViewModel
            
            Result.Success(session)
        } catch (e: Exception) {
            Result.Error(e, "Failed to accept transfer: ${e.message}")
        }
    }
    
    override fun getTransferState(sessionId: SessionId): Flow<TransferState> {
        return _transferStates.filter { it.sessionId == sessionId }
    }
    
    override suspend fun pauseTransfer(sessionId: SessionId) {
        activeSessions[sessionId]?.let { session ->
            session.isPaused = true
            session.job?.cancelChildren()
            transferRepository.updateSessionStatus(sessionId, TransferStatus.PAUSED)
            
            _transferStates.emit(
                TransferState(
                    sessionId = sessionId,
                    status = TransferStatus.PAUSED,
                    canResume = true
                )
            )
        }
    }
    
    override suspend fun resumeTransfer(sessionId: SessionId): Boolean {
        val sessionData = transferRepository.getSession(sessionId) ?: return false
        val transport = connectionManager.getTransport(sessionId)
        
        if (transport == null) {
            // Need to reconnect
            val reconnectResult = connectionManager.reconnect(sessionId, sessionData.targetDevice)
            if (reconnectResult.isError) {
                return false
            }
            
            val newTransport = reconnectResult.getOrNull()!!
            startTransferSession(sessionData, newTransport, resume = true)
        } else {
            // Resume with existing transport
            startTransferSession(sessionData, transport, resume = true)
        }
        
        return true
    }
    
    override suspend fun cancelTransfer(sessionId: SessionId) {
        activeSessions[sessionId]?.let { session ->
            session.isCancelled = true
            session.job?.cancel()
            
            connectionManager.closeConnection(sessionId)
            transferRepository.updateSessionStatus(sessionId, TransferStatus.CANCELLED)
            
            _transferStates.emit(
                TransferState(
                    sessionId = sessionId,
                    status = TransferStatus.CANCELLED,
                    canResume = false
                )
            )
            
            activeSessions.remove(sessionId)
        }
    }
    
    private suspend fun startTransferSession(
        session: TransferSession,
        transport: Transport,
        resume: Boolean = false
    ) {
        val job = CoroutineScope(dispatcherProvider.io).launch {
            try {
                transferRepository.updateSessionStatus(session.id, TransferStatus.IN_PROGRESS)
                
                // Load or create chunk map
                val chunkMap = if (resume) {
                    resumeStateManager.loadChunkState(session.id)
                        ?: createChunkMap(session.files)
                } else {
                    createChunkMap(session.files)
                }
                
                val activeSession = ActiveSession(
                    session = session,
                    transport = transport,
                    chunkMap = chunkMap,
                    job = this
                )
                
                activeSessions[session.id] = activeSession
                
                // Start parallel stream processing
                val streamJobs = (0 until SendraConstants.PARALLEL_STREAMS).map { streamIndex ->
                    launch {
                        processStream(streamIndex, activeSession)
                    }
                }
                
                // Progress monitor
                val progressJob = launch {
                    monitorProgress(activeSession)
                }
                
                // Wait for all streams to complete
                streamJobs.joinAll()
                progressJob.cancel()
                
                // Finalize
                if (!activeSession.isCancelled) {
                    if (chunkMap.isComplete()) {
                        finalizeTransfer(activeSession)
                    } else {
                        handleIncompleteTransfer(activeSession)
                    }
                }
                
            } catch (e: CancellationException) {
                Timber.d("Transfer cancelled: ${session.id}")
            } catch (e: Exception) {
                Timber.e(e, "Transfer failed: ${session.id}")
                handleTransferError(activeSession, e)
            } finally {
                activeSessions.remove(session.id)
            }
        }
    }
    
    private suspend fun processStream(streamIndex: Int, session: ActiveSession) {
        val dataChannel = session.transport.dataChannels[streamIndex]
        
        while (isActive && !session.isPaused && !session.isCancelled) {
            val nextChunk = session.chunkMap.getNextPendingChunk() ?: break
            
            try {
                // Update chunk status
                session.chunkMap.markInFlight(nextChunk.id)
                
                // Read file data
                val file = session.session.files[nextChunk.fileIndex]
                val chunkData = fileRepository.readChunk(
                    file = file,
                    offset = nextChunk.offset,
                    size = nextChunk.size
                )
                
                if (chunkData == null) {
                    session.chunkMap.markFailed(nextChunk.id)
                    continue
                }
                
                // Calculate CRC
                val crc = calculateCrc32(chunkData)
                
                // Create chunk
                val chunk = FileChunk(
                    id = nextChunk.id,
                    fileIndex = nextChunk.fileIndex,
                    chunkIndex = nextChunk.chunkIndex,
                    offset = nextChunk.offset,
                    size = chunkData.size,
                    data = chunkData,
                    crc32 = crc
                )
                
                // Send chunk
                val sendResult = dataChannel.sendChunk(chunk)
                if (sendResult.isError) {
                    session.chunkMap.markFailed(nextChunk.id)
                    continue
                }
                
                // Wait for ACK
                val ackResult = withTimeout(SendraConstants.CHUNK_TIMEOUT_MS) {
                    dataChannel.waitForAck(chunk.id, SendraConstants.CHUNK_TIMEOUT_MS)
                }
                
                if (ackResult.isSuccess) {
                    session.chunkMap.markCompleted(nextChunk.id)
                    session.bytesTransferred.addAndGet(chunkData.size.toLong())
                    
                    // Persist state periodically
                    if (session.chunkMap.getCompletedCount() % 10 == 0) {
                        resumeStateManager.saveChunkState(session.session.id, session.chunkMap)
                    }
                } else {
                    session.chunkMap.markFailed(nextChunk.id)
                    session.retryChunk(nextChunk.id)
                }
                
            } catch (e: TimeoutCancellationException) {
                Timber.w("Chunk timeout: ${nextChunk.id}")
                session.chunkMap.markFailed(nextChunk.id)
                session.retryChunk(nextChunk.id)
            } catch (e: Exception) {
                Timber.e(e, "Stream error on channel $streamIndex")
                throw e
            }
        }
    }
    
    private suspend fun monitorProgress(session: ActiveSession) {
        var lastBytes = 0L
        var lastTime = System.currentTimeMillis()
        
        while (isActive) {
            delay(SendraConstants.PROGRESS_UPDATE_INTERVAL_MS)
            
            val currentBytes = session.bytesTransferred.get()
            val currentTime = System.currentTimeMillis()
            
            val bytesDelta = currentBytes - lastBytes
            val timeDelta = currentTime - lastTime
            
            val speedBps = if (timeDelta > 0) {
                (bytesDelta * 1000) / timeDelta
            } else 0
            
            // Smooth speed
            session.smoothedSpeed = (session.smoothedSpeed * 0.7 + speedBps * 0.3).toLong()
            
            val remainingBytes = session.session.totalBytes - currentBytes
            val etaMs = if (session.smoothedSpeed > 0) {
                (remainingBytes * 1000) / session.smoothedSpeed
            } else 0
            
            val progress = TransferProgress(
                sessionId = session.session.id,
                bytesTransferred = currentBytes,
                totalBytes = session.session.totalBytes,
                currentFileIndex = session.chunkMap.getCurrentFileIndex(),
                totalFiles = session.session.files.size,
                currentFileBytesTransferred = session.chunkMap.getCurrentFileBytes(),
                chunksCompleted = session.chunkMap.getCompletedCount(),
                chunksTotal = session.chunkMap.getTotalChunks(),
                speedBytesPerSecond = session.smoothedSpeed,
                estimatedTimeRemainingMs = etaMs
            )
            
            _transferStates.emit(
                TransferState(
                    sessionId = session.session.id,
                    status = TransferStatus.IN_PROGRESS,
                    progress = progress
                )
            )
            
            lastBytes = currentBytes
            lastTime = currentTime
        }
    }
    
    private suspend fun finalizeTransfer(session: ActiveSession) {
        // Verify file integrity
        transferRepository.updateSessionStatus(session.session.id, TransferStatus.COMPLETED)
        resumeStateManager.clearChunkState(session.session.id)
        
        _transferStates.emit(
            TransferState(
                sessionId = session.session.id,
                status = TransferStatus.COMPLETED
            )
        )
        
        connectionManager.closeConnection(session.session.id)
    }
    
    private suspend fun handleIncompleteTransfer(session: ActiveSession) {
        transferRepository.updateSessionStatus(session.session.id, TransferStatus.INTERRUPTED)
        resumeStateManager.saveChunkState(session.session.id, session.chunkMap)
        
        _transferStates.emit(
            TransferState(
                sessionId = session.session.id,
                status = TransferStatus.INTERRUPTED,
                canResume = true
            )
        )
    }
    
    private suspend fun handleTransferError(session: ActiveSession, error: Exception) {
        transferRepository.updateSessionStatus(session.session.id, TransferStatus.FAILED)
        resumeStateManager.saveChunkState(session.session.id, session.chunkMap)
        
        _transferStates.emit(
            TransferState(
                sessionId = session.session.id,
                status = TransferStatus.FAILED,
                error = TransferError.Unknown(error, error.message ?: "Transfer failed"),
                canResume = error is IOException // Network errors are recoverable
            )
        )
        
        connectionManager.closeConnection(session.session.id)
    }
    
    private fun createChunkMap(files: List<FileInfo>): ChunkMap {
        val chunks = mutableListOf<ChunkInfo>()
        var globalChunkIndex = 0
        
        files.forEachIndexed { fileIndex, file ->
            val fileChunks = (file.size / SendraConstants.DEFAULT_CHUNK_SIZE).toInt() +
                if (file.size % SendraConstants.DEFAULT_CHUNK_SIZE > 0) 1 else 0
            
            repeat(fileChunks) { chunkIndex ->
                val offset = chunkIndex * SendraConstants.DEFAULT_CHUNK_SIZE
                val size = minOf(
                    SendraConstants.DEFAULT_CHUNK_SIZE.toInt(),
                    (file.size - offset).toInt()
                )
                
                chunks.add(
                    ChunkInfo(
                        id = "${fileIndex}_${chunkIndex}",
                        fileIndex = fileIndex,
                        chunkIndex = chunkIndex,
                        offset = offset,
                        size = size,
                        status = ChunkStatus.PENDING
                    )
                )
                globalChunkIndex++
            }
        }
        
        return ChunkMap(chunks)
    }
    
    private fun generateSessionId(): String {
        return java.util.UUID.randomUUID().toString().take(8)
    }
    
    private fun calculateCrc32(data: ByteArray): Long {
        val crc = java.util.zip.CRC32()
        crc.update(data)
        return crc.value
    }
    
    // Inner classes
    data class ActiveSession(
        val session: TransferSession,
        val transport: Transport,
        val chunkMap: ChunkMap,
        val job: Job? = null,
        val bytesTransferred: AtomicLong = AtomicLong(0),
        var smoothedSpeed: Long = 0,
        @Volatile var isPaused: Boolean = false,
        @Volatile var isCancelled: Boolean = false
    ) {
        private val retryCount = mutableMapOf<String, Int>()
        
        fun retryChunk(chunkId: String): Boolean {
            val currentRetries = retryCount.getOrDefault(chunkId, 0)
            if (currentRetries < 3) {
                retryCount[chunkId] = currentRetries + 1
                chunkMap.markPending(chunkId) // Reset to pending for retry
                return true
            }
            return false
        }
    }
    
    data class ChunkInfo(
        val id: String,
        val fileIndex: Int,
        val chunkIndex: Int,
        val offset: Long,
        val size: Int,
        var status: ChunkStatus
    )
    
    class ChunkMap(private val chunks: MutableList<ChunkInfo>) {
        private val chunkById = chunks.associateBy { it.id }.toMutableMap()
        private val pendingQueue = java.util.concurrent.ConcurrentLinkedQueue<ChunkInfo>()
        
        init {
            chunks.filter { it.status == ChunkStatus.PENDING }.forEach {
                pendingQueue.offer(it)
            }
        }
        
        fun getNextPendingChunk(): ChunkInfo? {
            synchronized(pendingQueue) {
                return pendingQueue.poll()?.also {
                    it.status = ChunkStatus.IN_FLIGHT
                }
            }
        }
        
        fun markInFlight(id: String) {
            chunkById[id]?.status = ChunkStatus.IN_FLIGHT
        }
        
        fun markCompleted(id: String) {
            chunkById[id]?.status = ChunkStatus.COMPLETED
        }
        
        fun markFailed(id: String) {
            chunkById[id]?.status = ChunkStatus.FAILED
        }
        
        fun markPending(id: String) {
            chunkById[id]?.let {
                it.status = ChunkStatus.PENDING
                pendingQueue.offer(it)
            }
        }
        
        fun getCompletedCount(): Int = chunks.count { it.status == ChunkStatus.COMPLETED }
        fun getTotalChunks(): Int = chunks.size
        fun isComplete(): Boolean = chunks.all { it.status == ChunkStatus.COMPLETED }
        
        fun getCurrentFileIndex(): Int {
            return chunks.firstOrNull { it.status != ChunkStatus.COMPLETED }?.fileIndex 
                ?: chunks.lastOrNull()?.fileIndex ?: 0
        }
        
        fun getCurrentFileBytes(): Long {
            val currentFile = getCurrentFileIndex()
            return chunks
                .filter { it.fileIndex == currentFile && it.status == ChunkStatus.COMPLETED }
                .sumOf { it.size.toLong() }
        }
    }
}

interface TransferManager {
    val transferStates: SharedFlow<TransferState>
    
    suspend fun initiateTransfer(
        files: List<FileInfo>,
        targetDevice: Device,
        resumeFromInterrupted: Boolean = false
    ): Result<TransferSession>
    
    suspend fun acceptTransfer(
        sessionId: SessionId,
        files: List<FileInfo>,
        senderDevice: Device
    ): Result<TransferSession>
    
    fun getTransferState(sessionId: SessionId): Flow<TransferState>
    suspend fun pauseTransfer(sessionId: SessionId)
    suspend fun resumeTransfer(sessionId: SessionId): Boolean
    suspend fun cancelTransfer(sessionId: SessionId)
}
