package com.sendra.domain.usecase.discovery

import com.sendra.core.dispatcher.DispatcherProvider
import com.sendra.core.result.Result
import kotlinx.coroutines.withContext
import javax.inject.Inject

class StartDiscoveryUseCase @Inject constructor(
    private val discoveryManager: DiscoveryManager,
    private val dispatcherProvider: DispatcherProvider
) {
    suspend operator fun invoke(): Result<Unit> = withContext(dispatcherProvider.io) {
        try {
            discoveryManager.startDiscovery()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Failed to start discovery: ${e.message}")
        }
    }
}

// DiscoveryManager interface defined in domain
interface DiscoveryManager {
    fun startDiscovery()
    fun stopDiscovery()
    fun isDiscoveryActive(): Boolean
    fun getDiscoveredDevices(): kotlinx.coroutines.flow.Flow<List<com.sendra.domain.model.Device>>
    suspend fun refreshDiscovery()
}
