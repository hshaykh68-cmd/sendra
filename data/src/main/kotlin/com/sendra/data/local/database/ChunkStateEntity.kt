package com.sendra.data.local.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chunk_states")
data class ChunkStateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: String,
    val fileIndex: Int,
    val chunkIndex: Int,
    val offset: Long,
    val size: Int,
    val status: String, // PENDING, IN_FLIGHT, COMPLETED, FAILED
    val retryCount: Int = 0
)
