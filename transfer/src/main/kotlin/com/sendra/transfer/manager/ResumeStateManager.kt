package com.sendra.transfer.manager

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ResumeStateManager @Inject constructor() {

    private val inMemoryChunkStates = mutableMapOf<String, TransferManagerImpl.ChunkMap>()

    fun loadChunkState(sessionId: String): TransferManagerImpl.ChunkMap? {
        return inMemoryChunkStates[sessionId]
    }

    fun saveChunkState(sessionId: String, chunkMap: TransferManagerImpl.ChunkMap) {
        inMemoryChunkStates[sessionId] = chunkMap
    }

    fun clearChunkState(sessionId: String) {
        inMemoryChunkStates.remove(sessionId)
    }
}
