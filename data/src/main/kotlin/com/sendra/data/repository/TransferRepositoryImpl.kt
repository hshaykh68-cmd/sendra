package com.sendra.data.repository

import com.sendra.data.local.database.*
import com.sendra.domain.model.*
import com.sendra.domain.repository.ChunkBitmap
import com.sendra.domain.repository.TransferRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransferRepositoryImpl @Inject constructor(
    private val transferDao: TransferDao,
    private val chunkStateDao: ChunkStateDao,
    private val historyDao: HistoryDao
) : TransferRepository {

    override suspend fun createSession(session: TransferSession) {
        val entity = TransferSessionEntity(
            id = session.id,
            direction = session.direction.name,
            status = session.status.name,
            deviceId = session.targetDevice.id,
            deviceName = session.targetDevice.name,
            totalBytes = session.totalBytes,
            bytesTransferred = 0,
            fileCount = session.files.size,
            connectionMethod = session.connectionMethod.name,
            createdAt = session.createdAt,
            updatedAt = System.currentTimeMillis()
        )
        transferDao.insertSession(entity)
    }

    override suspend fun updateSessionStatus(sessionId: SessionId, status: TransferStatus) {
        transferDao.updateStatus(sessionId, status.name)
    }

    override suspend fun getSession(sessionId: SessionId): TransferSession? {
        return transferDao.getSession(sessionId)?.toTransferSession()
    }

    override suspend fun deleteSession(sessionId: SessionId) {
        transferDao.deleteSession(sessionId)
    }

    override suspend fun saveChunkState(sessionId: SessionId, chunkState: ChunkState) {
        val entity = ChunkStateEntity(
            sessionId = sessionId,
            chunkId = chunkState.chunkId,
            fileIndex = 0, // Not tracked in ChunkState model
            chunkIndex = 0, // Not tracked in ChunkState model
            status = chunkState.status.name,
            retryCount = chunkState.retryCount,
            lastAttempt = chunkState.lastAttempt
        )
        chunkStateDao.insertChunkState(entity)
    }

    override suspend fun getChunkStates(sessionId: SessionId): List<ChunkState> {
        return chunkStateDao.getChunkStates(sessionId).map { entity ->
            ChunkState(
                chunkId = entity.chunkId,
                status = ChunkStatus.valueOf(entity.status),
                retryCount = entity.retryCount,
                lastAttempt = entity.lastAttempt
            )
        }
    }

    override suspend fun getChunkBitmap(sessionId: SessionId): ChunkBitmap? {
        // Calculate based on chunk states
        val chunks = getChunkStates(sessionId)
        if (chunks.isEmpty()) return null
        
        val totalChunks = chunks.size
        val bitmap = ChunkBitmap(totalChunks)
        chunks.forEachIndexed { index, chunk ->
            if (chunk.status == ChunkStatus.COMPLETED) {
                bitmap.markCompleted(index)
            }
        }
        return bitmap
    }

    override suspend fun clearChunkStates(sessionId: SessionId) {
        chunkStateDao.clearChunkStates(sessionId)
    }

    override suspend fun updateProgress(sessionId: SessionId, bytesTransferred: Long) {
        transferDao.updateProgress(sessionId, bytesTransferred)
    }

    override fun getProgressFlow(sessionId: SessionId): Flow<TransferProgress> {
        return transferDao.getSessionFlow(sessionId).map { entity ->
            entity?.let {
                TransferProgress(
                    sessionId = sessionId,
                    bytesTransferred = it.bytesTransferred,
                    totalBytes = it.totalBytes,
                    currentFileIndex = 0,
                    totalFiles = it.fileCount,
                    currentFileBytesTransferred = 0,
                    chunksCompleted = 0,
                    chunksTotal = 0
                )
            } ?: TransferProgress(
                sessionId = sessionId,
                bytesTransferred = 0,
                totalBytes = 0,
                currentFileIndex = 0,
                totalFiles = 0,
                currentFileBytesTransferred = 0,
                chunksCompleted = 0,
                chunksTotal = 0
            )
        }
    }

    override fun getTransferHistory(): Flow<List<TransferSession>> {
        return transferDao.getActiveSessions().map { entities ->
            entities.map { it.toTransferSession() }
        }
    }

    override fun getTransferHistoryForDevice(deviceId: DeviceId): Flow<List<TransferSession>> {
        // Filter active sessions by device
        return transferDao.getActiveSessions().map { entities ->
            entities.filter { it.deviceId == deviceId }.map { it.toTransferSession() }
        }
    }

    override suspend fun getFailedTransfers(): List<TransferSession> {
        return transferDao.getInterruptedSessions()
            .filter { it.status == TransferStatus.FAILED.name }
            .map { it.toTransferSession() }
    }

    override suspend fun clearOldHistory(olderThanMillis: Long) {
        val cutoff = System.currentTimeMillis() - olderThanMillis
        historyDao.deleteOldHistory(cutoff)
    }

    override fun getActiveSessions(): Flow<List<TransferSession>> {
        return transferDao.getActiveSessions().map { entities ->
            entities.map { it.toTransferSession() }
        }
    }

    override suspend fun getInterruptedSessions(): List<TransferSession> {
        return transferDao.getInterruptedSessions().map { it.toTransferSession() }
    }

    private fun TransferSessionEntity.toTransferSession(): TransferSession {
        return TransferSession(
            id = id,
            direction = TransferDirection.valueOf(direction),
            status = TransferStatus.valueOf(status),
            targetDevice = Device(
                id = deviceId,
                name = deviceName,
                type = DeviceType.UNKNOWN,
                capabilities = DeviceCapabilities(),
                connectionInfo = ConnectionInfo(
                    ipAddress = "",
                    port = 0,
                    connectionMethod = ConnectionMethod.valueOf(connectionMethod)
                )
            ),
            files = emptyList(), // Not stored in entity
            totalBytes = totalBytes,
            createdAt = createdAt,
            connectionMethod = ConnectionMethod.valueOf(connectionMethod),
            parentSessionId = parentSessionId
        )
    }
}
