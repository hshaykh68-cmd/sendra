package com.sendra.data.repository

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.OpenableColumns
import com.sendra.domain.model.FileInfo
import com.sendra.domain.repository.FileRepository
import com.sendra.domain.repository.StorageInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : FileRepository {
    
    private val contentResolver: ContentResolver = context.contentResolver
    private val receiveDirectory: File by lazy {
        File(context.getExternalFilesDir(null), "Sendra/Received").apply {
            if (!exists()) mkdirs()
        }
    }
    
    override suspend fun getFileInfo(uri: String): FileInfo? = withContext(Dispatchers.IO) {
        try {
            val fileUri = Uri.parse(uri)
            
            // Query file metadata
            val cursor = contentResolver.query(fileUri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val displayNameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                    
                    val name = if (displayNameIndex >= 0) it.getString(displayNameIndex) else "Unknown"
                    val size = if (sizeIndex >= 0) it.getLong(sizeIndex) else 0L
                    
                    val mimeType = contentResolver.getType(fileUri) ?: "*/*"
                    
                    FileInfo(
                        id = uri,
                        name = name,
                        size = size,
                        mimeType = mimeType,
                        path = uri
                    )
                } else null
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get file info for $uri")
            null
        }
    }
    
    override suspend fun readFileChunk(
        fileUri: String,
        offset: Long,
        length: Int
    ): ByteArray? = withContext(Dispatchers.IO) {
        try {
            contentResolver.openFileDescriptor(Uri.parse(fileUri), "r")?.use { pfd ->
                FileInputStream(pfd.fileDescriptor).use { stream ->
                    stream.skip(offset)
                    val buffer = ByteArray(length)
                    val bytesRead = stream.read(buffer)
                    if (bytesRead > 0) {
                        if (bytesRead == length) {
                            buffer
                        } else {
                            buffer.copyOf(bytesRead)
                        }
                    } else null
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to read file chunk at offset $offset")
            null
        }
    }
    
    override suspend fun writeFileChunk(
        fileUri: String,
        offset: Long,
        data: ByteArray
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = File(Uri.parse(fileUri).path ?: return@withContext Result.failure(
                IllegalArgumentException("Invalid file URI")
            ))
            
            RandomAccessFile(file, "rw").use { raf ->
                raf.seek(offset)
                raf.write(data)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to write file chunk at offset $offset")
            Result.failure(e)
        }
    }
    
    override suspend fun createDestinationFile(
        fileName: String,
        mimeType: String,
        totalSize: Long
    ): String = withContext(Dispatchers.IO) {
        // Ensure unique filename
        var file = File(receiveDirectory, fileName)
        var counter = 1
        val nameWithoutExt = fileName.substringBeforeLast(".")
        val extension = fileName.substringAfterLast(".", "")
        
        while (file.exists()) {
            val newName = if (extension.isNotEmpty()) {
                "$nameWithoutExt ($counter).$extension"
            } else {
                "$nameWithoutExt ($counter)"
            }
            file = File(receiveDirectory, newName)
            counter++
        }
        
        // Pre-allocate space on Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                file.createNewFile()
                file.setWritable(true)
            } catch (e: Exception) {
                Timber.e(e, "Failed to pre-allocate file")
            }
        } else {
            file.createNewFile()
        }
        
        Uri.fromFile(file).toString()
    }
    
    override suspend fun deleteFile(fileUri: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = File(Uri.parse(fileUri).path ?: return@withContext Result.failure(
                IllegalArgumentException("Invalid file URI")
            ))
            
            if (file.exists() && file.delete()) {
                Result.success(Unit)
            } else {
                Result.failure(IOException("Failed to delete file"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getFilesInDirectory(directoryUri: String): List<FileInfo> = withContext(Dispatchers.IO) {
        try {
            val directory = File(Uri.parse(directoryUri).path ?: return@withContext emptyList())
            directory.listFiles()?.map { file ->
                FileInfo(
                    id = Uri.fromFile(file).toString(),
                    name = file.name,
                    size = file.length(),
                    mimeType = getMimeTypeFromExtension(file.extension),
                    path = file.absolutePath
                )
            } ?: emptyList()
        } catch (e: Exception) {
            Timber.e(e, "Failed to list files in directory")
            emptyList()
        }
    }
    
    override fun observeStorageSpace(): Flow<StorageInfo> = flow {
        while (true) {
            val stat = android.os.StatFs(receiveDirectory.absolutePath)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availableBlocks = stat.availableBlocksLong
            
            emit(
                StorageInfo(
                    totalSpace = totalBlocks * blockSize,
                    freeSpace = availableBlocks * blockSize,
                    usedSpace = (totalBlocks - availableBlocks) * blockSize
                )
            )
            
            kotlinx.coroutines.delay(5000) // Update every 5 seconds
        }
    }
    
    private fun getMimeTypeFromExtension(extension: String): String {
        return when (extension.lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "mp4" -> "video/mp4"
            "mkv" -> "video/x-matroska"
            "avi" -> "video/x-msvideo"
            "mov" -> "video/quicktime"
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "pdf" -> "application/pdf"
            "doc", "docx" -> "application/msword"
            "xls", "xlsx" -> "application/vnd.ms-excel"
            "ppt", "pptx" -> "application/vnd.ms-powerpoint"
            "txt" -> "text/plain"
            "zip" -> "application/zip"
            else -> "*/*"
        }
    }
}
