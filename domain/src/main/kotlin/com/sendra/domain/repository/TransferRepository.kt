package com.sendra.domain.repository

import com.sendra.domain.model.*
import kotlinx.coroutines.flow.Flow

interface TransferRepository {
    // Session lifecycle
    suspend fun createSession(session: TransferSession)
    suspend fun updateSessionStatus(sessionId: SessionId, status: TransferStatus)
    suspend fun getSession(sessionId: SessionId): TransferSession?
    suspend fun deleteSession(sessionId: SessionId)
    
    // Chunk state for resume
    suspend fun saveChunkState(sessionId: SessionId, chunkState: ChunkState)
    suspend fun getChunkStates(sessionId: SessionId): List<ChunkState>
    suspend fun getChunkBitmap(sessionId: SessionId): ChunkBitmap?
    suspend fun clearChunkStates(sessionId: SessionId)
    
    // Progress tracking
    suspend fun updateProgress(sessionId: SessionId, bytesTransferred: Long)
    fun getProgressFlow(sessionId: SessionId): Flow<TransferProgress>
    
    // History
    fun getTransferHistory(): Flow<List<TransferSession>>
    fun getTransferHistoryForDevice(deviceId: DeviceId): Flow<List<TransferSession>>
    suspend fun getFailedTransfers(): List<TransferSession>
    suspend fun clearOldHistory(olderThanMillis: Long)
    
    // Active sessions
    fun getActiveSessions(): Flow<List<TransferSession>>
    suspend fun getInterruptedSessions(): List<TransferSession>
}

// ChunkBitmap for efficient resume state tracking
class ChunkBitmap(
    val totalChunks: Int,
    private val bits: java.util.BitSet = java.util.BitSet(totalChunks)
) {
    fun markCompleted(chunkIndex: Int) {
        bits.set(chunkIndex)
    }
    
    fun isCompleted(chunkIndex: Int): Boolean = bits.get(chunkIndex)
    
    fun getCompletedCount(): Int = bits.cardinality()
    
    fun getPendingIndices(): List<Int> {
        return (0 until totalChunks).filterNot { bits.get(it) }
    }
    
    fun isComplete(): Boolean = bits.cardinality() == totalChunks
    
    fun toByteArray(): ByteArray {
        return bits.toByteArray()
    }
    
    companion object {
        fun fromByteArray(bytes: ByteArray, totalChunks: Int): ChunkBitmap {
            val bits = java.util.BitSet.valueOf(bytes)
            return ChunkBitmap(totalChunks, bits)
        }
    }
}
