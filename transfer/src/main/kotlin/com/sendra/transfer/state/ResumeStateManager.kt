package com.sendra.transfer.state

import com.sendra.core.dispatcher.DispatcherProvider
import com.sendra.data.local.database.ChunkStateDao
import com.sendra.data.local.database.ChunkStateEntity
import com.sendra.domain.model.SessionId
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ResumeStateManager @Inject constructor(
    private val chunkStateDao: ChunkStateDao,
    private val dispatcherProvider: DispatcherProvider
) {
    suspend fun saveChunkState(sessionId: SessionId, chunkMap: TransferManagerImpl.ChunkMap) {
        withContext(dispatcherProvider.io) {
            // Convert chunk map to entities
            val entities = chunkMap.toChunkEntities(sessionId)
            chunkStateDao.insertChunkStates(entities)
        }
    }
    
    suspend fun loadChunkState(sessionId: SessionId): TransferManagerImpl.ChunkMap? {
        return withContext(dispatcherProvider.io) {
            val entities = chunkStateDao.getChunkStates(sessionId)
            if (entities.isEmpty()) return@withContext null
            
            // Reconstruct chunk map from entities
            entities.toChunkMap()
        }
    }
    
    suspend fun clearChunkState(sessionId: SessionId) {
        withContext(dispatcherProvider.io) {
            chunkStateDao.clearChunkStates(sessionId)
            
            // Also clear temp file if exists
            val tempFile = File(System.getProperty("java.io.tmpdir"), "sendra_resume_$sessionId.bin")
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }
    }
    
    suspend fun hasResumableState(sessionId: SessionId): Boolean {
        return withContext(dispatcherProvider.io) {
            chunkStateDao.getChunkStates(sessionId).isNotEmpty()
        }
    }
    
    private fun TransferManagerImpl.ChunkMap.toChunkEntities(sessionId: String): List<ChunkStateEntity> {
        // This would need access to internal chunks list
        // Implementation depends on ChunkMap structure
        return emptyList() // Placeholder
    }
    
    private fun List<ChunkStateEntity>.toChunkMap(): TransferManagerImpl.ChunkMap? {
        // Reconstruct from entities
        return null // Placeholder
    }
}
