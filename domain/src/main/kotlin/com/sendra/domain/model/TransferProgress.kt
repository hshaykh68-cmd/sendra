package com.sendra.domain.model

data class TransferProgress(
    val sessionId: SessionId,
    val bytesTransferred: Long,
    val totalBytes: Long,
    val currentFileIndex: Int,
    val totalFiles: Int,
    val currentFileBytesTransferred: Long,
    val chunksCompleted: Int,
    val chunksTotal: Int,
    val speedBytesPerSecond: Long = 0,
    val estimatedTimeRemainingMs: Long = 0
) {
    val percent: Int = if (totalBytes > 0) {
        ((bytesTransferred * 100) / totalBytes).toInt().coerceIn(0, 100)
    } else 0
    
    val isComplete: Boolean = bytesTransferred >= totalBytes && totalBytes > 0
    
    val formattedSpeed: String
        get() = when {
            speedBytesPerSecond >= 1_000_000_000 -> "%.1f GB/s".format(speedBytesPerSecond / 1_000_000_000.0)
            speedBytesPerSecond >= 1_000_000 -> "%.1f MB/s".format(speedBytesPerSecond / 1_000_000.0)
            speedBytesPerSecond >= 1_000 -> "%.1f KB/s".format(speedBytesPerSecond / 1_000.0)
            else -> "$speedBytesPerSecond B/s"
        }
    
    val formattedEta: String
        get() = when {
            estimatedTimeRemainingMs <= 0 || isComplete -> "Done"
            estimatedTimeRemainingMs < 60_000 -> "${estimatedTimeRemainingMs / 1000}s"
            estimatedTimeRemainingMs < 3_600_000 -> "${estimatedTimeRemainingMs / 60_000}m ${(estimatedTimeRemainingMs % 60_000) / 1000}s"
            else -> "${estimatedTimeRemainingMs / 3_600_000}h ${(estimatedTimeRemainingMs % 3_600_000) / 60_000}m"
        }
}

data class TransferState(
    val sessionId: SessionId,
    val status: TransferStatus,
    val progress: TransferProgress? = null,
    val currentFile: FileInfo? = null,
    val error: TransferError? = null,
    val canResume: Boolean = false
)

sealed class TransferError(
    open val message: String,
    open val retryable: Boolean = true
) {
    data class ConnectionLost(
        override val message: String = "Connection lost",
        val willRetry: Boolean = true
    ) : TransferError(message, willRetry)
    
    data class StorageFull(
        val requiredBytes: Long,
        val availableBytes: Long,
        override val message: String = "Not enough storage space"
    ) : TransferError(message, retryable = false)
    
    data class FileNotFound(
        val filePath: String,
        override val message: String = "File not found"
    ) : TransferError(message, retryable = false)
    
    data class IntegrityCheckFailed(
        val chunkId: ChunkId? = null,
        override val message: String = "File integrity check failed"
    ) : TransferError(message)
    
    data class Unknown(
        val cause: Throwable? = null,
        override val message: String = "Transfer failed"
    ) : TransferError(message)
}
