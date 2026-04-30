package com.sendra.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class TransferSession(
    val id: SessionId,
    val direction: TransferDirection,
    val status: TransferStatus,
    val targetDevice: Device,
    val files: List<FileInfo>,
    val totalBytes: Long = files.sumOf { it.size },
    val createdAt: Long = System.currentTimeMillis(),
    val connectionMethod: ConnectionMethod = ConnectionMethod.UNKNOWN,
    val parentSessionId: SessionId? = null // For resumed transfers
) {
    val isActive: Boolean
        get() = status == TransferStatus.PREPARING || 
                status == TransferStatus.IN_PROGRESS || 
                status == TransferStatus.PAUSED ||
                status == TransferStatus.RECONNECTING
}

typealias SessionId = String
typealias ChunkId = String

enum class TransferDirection {
    SEND,
    RECEIVE
}

enum class TransferStatus {
    PREPARING,
    IN_PROGRESS,
    PAUSED,
    RECONNECTING,
    COMPLETED,
    FAILED,
    CANCELLED,
    INTERRUPTED
}

@Serializable
data class FileInfo(
    val id: FileId,
    val name: String,
    val size: Long,
    val mimeType: String,
    val path: String? = null, // Local path if known
    val hash: String? = null, // SHA-256 prefix
    val thumbnailPath: String? = null
)

typealias FileId = String

data class FileChunk(
    val id: ChunkId,
    val fileIndex: Int,
    val chunkIndex: Int,
    val offset: Long,
    val size: Int,
    val data: ByteArray? = null, // Null for metadata-only
    val crc32: Long = 0
) {
    companion object {
        fun generateId(sessionId: SessionId, fileIndex: Int, chunkIndex: Int): String =
            "${sessionId}_${fileIndex}_$chunkIndex"
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as FileChunk
        return id == other.id
    }
    
    override fun hashCode(): Int = id.hashCode()
}

enum class ChunkStatus {
    PENDING,
    IN_FLIGHT,
    COMPLETED,
    FAILED
}

data class ChunkState(
    val chunkId: ChunkId,
    val status: ChunkStatus,
    val retryCount: Int = 0,
    val lastAttempt: Long? = null
)
