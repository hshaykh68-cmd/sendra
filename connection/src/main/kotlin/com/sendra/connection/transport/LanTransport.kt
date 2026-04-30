package com.sendra.connection.transport

import com.sendra.core.constants.SendraConstants
import com.sendra.core.dispatcher.DispatcherProvider
import com.sendra.core.result.Result
import com.sendra.domain.model.ChunkId
import com.sendra.domain.model.FileChunk
import kotlinx.coroutines.*
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Semaphore
import timber.log.Timber
import java.io.*
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.CRC32

class LanTransport(
    private val dispatcherProvider: DispatcherProvider
) : Transport {
    
    private var socket: Socket? = null
    private var dataSockets: List<Socket> = emptyList()
    
    override val controlChannel: ControlChannel = LanControlChannel()
    override val dataChannels: List<DataChannel> = (0 until SendraConstants.PARALLEL_STREAMS).map {
        LanDataChannel(streamId = it)
    }
    
    override var isConnected: Boolean = false
        private set
    
    override val connectionMethod = com.sendra.domain.model.ConnectionMethod.LAN
    
    private val _connectionState = MutableStateFlow(TransportConnectionState.DISCONNECTED)
    override fun connectionState(): StateFlow<TransportConnectionState> = _connectionState.asStateFlow()
    
    override suspend fun connect(endpoint: Endpoint): Result<Unit> = withContext(dispatcherProvider.io) {
        try {
            Timber.d("Connecting to ${endpoint.host}:${endpoint.port}")
            _connectionState.value = TransportConnectionState.CONNECTING
            
            // Main control socket
            socket = Socket().apply {
                connect(java.net.InetSocketAddress(endpoint.host, endpoint.port), SendraConstants.CONNECTION_TIMEOUT_MS.toInt())
                sendBufferSize = SendraConstants.SOCKET_BUFFER_SIZE
                receiveBufferSize = SendraConstants.SOCKET_BUFFER_SIZE
                keepAlive = true
            }
            
            // Data sockets for parallel streams
            dataSockets = (0 until SendraConstants.PARALLEL_STREAMS).map { index ->
                Socket().apply {
                    connect(java.net.InetSocketAddress(endpoint.host, endpoint.port + index + 1), SendraConstants.CONNECTION_TIMEOUT_MS.toInt())
                    sendBufferSize = SendraConstants.SOCKET_BUFFER_SIZE
                    receiveBufferSize = SendraConstants.SOCKET_BUFFER_SIZE
                    keepAlive = true
                }
            }
            
            // Assign sockets to channels
            (dataChannels as List<LanDataChannel>).forEachIndexed { index, channel ->
                channel.attachSocket(dataSockets[index])
            }
            
            isConnected = true
            _connectionState.value = TransportConnectionState.CONNECTED
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "LAN connection failed")
            _connectionState.value = TransportConnectionState.ERROR
            Result.Error(e, "Failed to establish LAN connection: ${e.message}")
        }
    }
    
    override suspend fun disconnect() {
        withContext(dispatcherProvider.io) {
            isConnected = false
            _connectionState.value = TransportConnectionState.DISCONNECTED
            
            dataSockets.forEach { it.closeQuietly() }
            socket?.closeQuietly()
            
            dataSockets = emptyList()
            socket = null
        }
    }
    
    private fun Socket.closeQuietly() {
        try {
            close()
        } catch (e: Exception) {
            // Ignore
        }
    }
    
    // Control Channel Implementation
    private inner class LanControlChannel : ControlChannel {
        
        override suspend fun send(command: ControlCommand): Result<Unit> = withContext(dispatcherProvider.io) {
            try {
                val out = socket?.getOutputStream() ?: return@withContext Result.Error(
                    IllegalStateException("Socket not connected"),
                    "Control channel not connected"
                )
                
                val data = serializeCommand(command)
                out.write(data)
                out.flush()
                
                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error(e, "Failed to send control command: ${e.message}")
            }
        }
        
        override fun receive(): Flow<ControlEvent> = kotlinx.coroutines.flow.flow {
            val input = socket?.getInputStream() ?: return@flow
            val reader = BufferedReader(InputStreamReader(input))

            while (currentCoroutineContext().isActive && isConnected) {
                try {
                    val line = reader.readLine() ?: break
                    val event = deserializeEvent(line)
                    emit(event)
                } catch (e: Exception) {
                    Timber.e(e, "Error reading control event")
                    break
                }
            }
        }
        
        override suspend fun sendHandshake(
            sessionId: String,
            capabilities: List<String>
        ): Result<HandshakeResponse> = withContext(dispatcherProvider.io) {
            try {
                // Send handshake
                val handshakeData = "HANDSHAKE|v1|${capabilities.joinToString(",")}|$sessionId\n"
                socket?.getOutputStream()?.write(handshakeData.toByteArray())
                
                // Read response
                val reader = BufferedReader(InputStreamReader(socket?.getInputStream()))
                val response = reader.readLine() ?: return@withContext Result.Error(
                    IOException("No handshake response"),
                    "Handshake failed"
                )
                
                // Parse response
                val parts = response.split("|")
                if (parts.size >= 4 && parts[0] == "HANDSHAKE_ACK") {
                    Result.Success(HandshakeResponse(
                        accepted = parts[1] == "OK",
                        protocolVersion = parts[2].toIntOrNull() ?: 1,
                        capabilities = parts[3].split(","),
                        receiveBufferSize = parts.getOrNull(4)?.toIntOrNull() ?: 65536
                    ))
                } else {
                    Result.Error(IOException("Invalid handshake response"), "Handshake failed")
                }
            } catch (e: Exception) {
                Result.Error(e, "Handshake error: ${e.message}")
            }
        }
        
        private fun serializeCommand(command: ControlCommand): ByteArray {
            val str = when (command) {
                is ControlCommand.StartTransfer -> "START|${command.sessionId}|${command.fileManifest.size}"
                is ControlCommand.PauseTransfer -> "PAUSE|${command.sessionId}"
                is ControlCommand.ResumeTransfer -> "RESUME|${command.sessionId}"
                is ControlCommand.CancelTransfer -> "CANCEL|${command.sessionId}"
                is ControlCommand.ChunkAck -> "ACK|${command.chunkId}|${command.status}"
                ControlCommand.Ping -> "PING"
            }
            return "$str\n".toByteArray()
        }
        
        private fun deserializeEvent(line: String): ControlEvent {
            val parts = line.split("|")
            return when (parts.getOrNull(0)) {
                "ACCEPTED" -> ControlEvent.TransferAccepted(
                    parts.getOrElse(1) { "" },
                    parts.getOrNull(2)?.toIntOrNull() ?: SendraConstants.DEFAULT_TRANSFER_PORT
                )
                "REJECTED" -> ControlEvent.TransferRejected(
                    parts.getOrElse(1) { "" },
                    parts.getOrElse(2) { "Unknown" }
                )
                "PAUSED" -> ControlEvent.TransferPaused(parts.getOrElse(1) { "" })
                "RESUMED" -> ControlEvent.TransferResumed(parts.getOrElse(1) { "" })
                "CANCELLED" -> ControlEvent.TransferCancelled(parts.getOrElse(1) { "" })
                "PONG" -> ControlEvent.Pong
                else -> ControlEvent.Error("Unknown event: $line")
            }
        }
    }
    
    // Data Channel Implementation
    private inner class LanDataChannel(val streamId: Int) : DataChannel {
        private var dataSocket: Socket? = null
        private val pendingAcks = ConcurrentHashMap<ChunkId, CompletableDeferred<Boolean>>()
        private val receiveSemaphore = Semaphore(1)
        
        fun attachSocket(socket: Socket) {
            this.dataSocket = socket
            startAckListener()
        }
        
        override suspend fun sendChunk(chunk: FileChunk): Result<Unit> = withContext(dispatcherProvider.io) {
            try {
                val socket = dataSocket ?: return@withContext Result.Error(
                    IllegalStateException("Data socket not connected"),
                    "Data channel $streamId not connected"
                )
                
                val out = DataOutputStream(BufferedOutputStream(socket.getOutputStream()))
                
                // Write chunk header
                out.writeUTF(chunk.id)
                out.writeInt(chunk.fileIndex)
                out.writeInt(chunk.chunkIndex)
                out.writeLong(chunk.offset)
                out.writeInt(chunk.size)
                out.writeLong(chunk.crc32)
                
                // Write chunk data
                chunk.data?.let { data ->
                    out.write(data)
                }
                
                out.flush()
                
                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error(e, "Failed to send chunk: ${e.message}")
            }
        }
        
        override suspend fun receiveChunk(): Result<FileChunk> = withContext(dispatcherProvider.io) {
            receiveSemaphore.acquire()
            try {
                val socket = dataSocket ?: return@withContext Result.Error(
                    IllegalStateException("Data socket not connected"),
                    "Data channel $streamId not connected"
                )
                
                val input = DataInputStream(BufferedInputStream(socket.getInputStream()))
                
                // Read header
                val id = input.readUTF()
                val fileIndex = input.readInt()
                val chunkIndex = input.readInt()
                val offset = input.readLong()
                val size = input.readInt()
                val crc32 = input.readLong()
                
                // Read data
                val data = ByteArray(size)
                input.readFully(data)
                
                // Verify CRC
                val calculatedCrc = calculateCrc32(data)
                if (calculatedCrc != crc32) {
                    return@withContext Result.Error(
                        ChecksumException("CRC mismatch"),
                        "Chunk integrity check failed"
                    )
                }
                
                Result.Success(FileChunk(
                    id = id,
                    fileIndex = fileIndex,
                    chunkIndex = chunkIndex,
                    offset = offset,
                    size = size,
                    data = data,
                    crc32 = crc32
                ))
            } catch (e: Exception) {
                Result.Error(e, "Failed to receive chunk: ${e.message}")
            } finally {
                receiveSemaphore.release()
            }
        }
        
        override suspend fun sendAck(chunkId: ChunkId): Result<Unit> = withContext(dispatcherProvider.io) {
            try {
                val out = dataSocket?.getOutputStream() ?: return@withContext Result.Error(
                    IllegalStateException("Socket not connected"),
                    "Cannot send ACK"
                )
                
                out.write("ACK|$chunkId|SUCCESS\n".toByteArray())
                out.flush()
                
                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error(e, "Failed to send ACK: ${e.message}")
            }
        }
        
        override suspend fun waitForAck(chunkId: ChunkId, timeoutMs: Long): Result<Unit> {
            val deferred = CompletableDeferred<Boolean>()
            pendingAcks[chunkId] = deferred
            
            return try {
                withTimeout(timeoutMs) {
                    val success = deferred.await()
                    if (success) {
                        Result.Success(Unit)
                    } else {
                        Result.Error(AckException("Negative ACK"), "Chunk rejected")
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Result.Error(TimeoutException("ACK timeout"), "No acknowledgment received")
            } finally {
                pendingAcks.remove(chunkId)
            }
        }
        
        override fun isOpen(): Boolean {
            return dataSocket?.isConnected == true && !dataSocket!!.isClosed
        }
        
        private fun startAckListener() {
            CoroutineScope(dispatcherProvider.io).launch {
                try {
                    val reader = BufferedReader(InputStreamReader(dataSocket?.getInputStream()))
                    while (isActive && isOpen()) {
                        val line = reader.readLine() ?: break
                        val parts = line.split("|")
                        if (parts.size >= 3 && parts[0] == "ACK") {
                            val chunkId = parts[1]
                            val success = parts[2] == "SUCCESS"
                            pendingAcks[chunkId]?.complete(success)
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "ACK listener error")
                }
            }
        }
        
        private fun calculateCrc32(data: ByteArray): Long {
            val crc32 = CRC32()
            crc32.update(data)
            return crc32.value
        }
    }
    
    class ChecksumException(message: String) : Exception(message)
    class AckException(message: String) : Exception(message)
    class TimeoutException(message: String) : Exception(message)
}
