package com.sendra.domain.usecase.discovery

import com.sendra.core.dispatcher.DispatcherProvider
import com.sendra.core.result.Result
import kotlinx.coroutines.withContext
import javax.inject.Inject

class StopDiscoveryUseCase @Inject constructor(
    private val discoveryManager: DiscoveryManager,
    private val dispatcherProvider: DispatcherProvider
) {
    suspend operator fun invoke(): Result<Unit> = withContext(dispatcherProvider.io) {
        try {
            discoveryManager.stopDiscovery()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Failed to stop discovery: ${e.message}")
        }
    }
}
