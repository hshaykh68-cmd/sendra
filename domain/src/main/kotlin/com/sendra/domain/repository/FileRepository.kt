package com.sendra.domain.repository

import com.sendra.domain.model.FileInfo
import kotlinx.coroutines.flow.Flow

interface FileRepository {
    /**
     * Get file information from URI
     */
    suspend fun getFileInfo(uri: String): FileInfo?
    
    /**
     * Read file chunk for sending
     */
    suspend fun readFileChunk(
        fileUri: String,
        offset: Long,
        length: Int
    ): ByteArray?
    
    /**
     * Write received chunk to file
     */
    suspend fun writeFileChunk(
        fileUri: String,
        offset: Long,
        data: ByteArray
    ): Result<Unit>
    
    /**
     * Create destination file for incoming transfer
     */
    suspend fun createDestinationFile(
        fileName: String,
        mimeType: String,
        totalSize: Long
    ): String
    
    /**
     * Delete a file
     */
    suspend fun deleteFile(fileUri: String): Result<Unit>
    
    /**
     * Get all files in a directory
     */
    suspend fun getFilesInDirectory(directoryUri: String): List<FileInfo>
    
    /**
     * Observe storage space
     */
    fun observeStorageSpace(): Flow<StorageInfo>
}

data class StorageInfo(
    val totalSpace: Long,
    val freeSpace: Long,
    val usedSpace: Long
) {
    val freeSpacePercent: Float = if (totalSpace > 0) {
        freeSpace.toFloat() / totalSpace.toFloat()
    } else 0f
}
