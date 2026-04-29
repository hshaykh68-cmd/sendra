package com.sendra.data.local.database

import androidx.room.*

@Dao
interface ChunkStateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunkState(chunk: ChunkStateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunkStates(chunks: List<ChunkStateEntity>)

    @Query("SELECT * FROM chunk_states WHERE sessionId = :sessionId ORDER BY fileIndex, chunkIndex")
    suspend fun getChunkStates(sessionId: String): List<ChunkStateEntity>

    @Query("DELETE FROM chunk_states WHERE sessionId = :sessionId")
    suspend fun clearChunkStates(sessionId: String)
}
