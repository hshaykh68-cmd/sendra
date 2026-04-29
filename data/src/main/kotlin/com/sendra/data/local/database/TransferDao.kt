package com.sendra.data.local.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "transfer_sessions")
data class TransferSessionEntity(
    @PrimaryKey
    val id: String,
    val direction: String, // SEND or RECEIVE
    val status: String,
    val deviceId: String,
    val deviceName: String,
    val totalBytes: Long,
    val bytesTransferred: Long = 0,
    val fileCount: Int,
    val connectionMethod: String,
    val createdAt: Long,
    val updatedAt: Long,
    val completedAt: Long? = null,
    val parentSessionId: String? = null,
    val errorMessage: String? = null
)

@Entity(
    tableName = "chunk_states",
    primaryKeys = ["sessionId", "chunkId"]
)
data class ChunkStateEntity(
    val sessionId: String,
    val chunkId: String,
    val fileIndex: Int,
    val chunkIndex: Int,
    val status: String, // PENDING, IN_FLIGHT, COMPLETED, FAILED
    val retryCount: Int = 0,
    val lastAttempt: Long? = null
)

@Entity(tableName = "transfer_history")
data class TransferHistoryEntity(
    @PrimaryKey
    val id: String,
    val sessionId: String,
    val timestamp: Long,
    val direction: String,
    val status: String,
    val deviceId: String,
    val deviceName: String,
    val totalBytes: Long,
    val bytesTransferred: Long,
    val fileCount: Int,
    val durationMs: Long? = null
)

@Dao
interface TransferDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: TransferSessionEntity)
    
    @Update
    suspend fun updateSession(session: TransferSessionEntity)
    
    @Query("UPDATE transfer_sessions SET status = :status, updatedAt = :timestamp WHERE id = :sessionId")
    suspend fun updateStatus(sessionId: String, status: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE transfer_sessions SET bytesTransferred = :bytes WHERE id = :sessionId")
    suspend fun updateProgress(sessionId: String, bytes: Long)
    
    @Query("SELECT * FROM transfer_sessions WHERE id = :sessionId")
    suspend fun getSession(sessionId: String): TransferSessionEntity?
    
    @Query("SELECT * FROM transfer_sessions WHERE status IN ('PREPARING', 'IN_PROGRESS', 'PAUSED', 'RECONNECTING')")
    fun getActiveSessions(): Flow<List<TransferSessionEntity>>
    
    @Query("SELECT * FROM transfer_sessions WHERE id = :sessionId")
    fun getSessionFlow(sessionId: String): Flow<TransferSessionEntity?>
    
    @Query("DELETE FROM transfer_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: String)
    
    // Chunk states
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunkState(chunk: ChunkStateEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunkStates(chunks: List<ChunkStateEntity>)
    
    @Query("SELECT * FROM chunk_states WHERE sessionId = :sessionId ORDER BY fileIndex, chunkIndex")
    suspend fun getChunkStates(sessionId: String): List<ChunkStateEntity>
    
    @Query("DELETE FROM chunk_states WHERE sessionId = :sessionId")
    suspend fun clearChunkStates(sessionId: String)
    
    // History
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(entry: TransferHistoryEntity)
    
    @Query("SELECT * FROM transfer_history ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentHistory(limit: Int = 100): List<TransferHistoryEntity>
    
    @Query("DELETE FROM transfer_history WHERE timestamp < :cutoff")
    suspend fun deleteOldHistory(cutoff: Long)
    
    @Query("SELECT * FROM transfer_sessions WHERE status = 'INTERRUPTED' OR status = 'FAILED'")
    suspend fun getInterruptedSessions(): List<TransferSessionEntity>
}
