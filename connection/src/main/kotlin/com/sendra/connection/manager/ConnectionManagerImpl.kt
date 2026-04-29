package com.sendra.connection.manager

import com.sendra.connection.transport.*
import com.sendra.core.constants.SendraConstants
import com.sendra.core.dispatcher.DispatcherProvider
import com.sendra.core.result.Result
import com.sendra.domain.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectionManagerImpl @Inject constructor(
    private val dispatcherProvider: DispatcherProvider,
    private val wifiDirectController: WifiDirectController,
    private val hotspotController: HotspotController
) : ConnectionManager {
    
    private val activeTransports = mutableMapOf<SessionId, Transport>()
    private val _connectionStates = MutableStateFlow<Map<SessionId, ConnectionState>>(emptyMap())
    
    override suspend fun establishConnection(
        sessionId: SessionId,
        device: Device,
        preferredMethod: ConnectionMethod?
    ): Result<Transport> = withContext(dispatcherProvider.io) {
        
        Timber.d("Establishing connection to ${device.name} via ${preferredMethod ?: "auto"}")
        
        // Try methods in priority order
        val methodsToTry = when (preferredMethod) {
            ConnectionMethod.LAN -> listOf(ConnectionMethod.LAN)
            ConnectionMethod.WIFI_DIRECT -> listOf(ConnectionMethod.WIFI_DIRECT, ConnectionMethod.HOTSPOT_SENDER)
            ConnectionMethod.HOTSPOT_SENDER -> listOf(ConnectionMethod.HOTSPOT_SENDER)
            else -> determineConnectionMethods(device)
        }
        
        for (method in methodsToTry) {
            val result = attemptConnection(sessionId, device, method)
            if (result.isSuccess) {
                return@withContext result
            }
            
            Timber.w("Connection method $method failed, trying next...")
            delay(SendraConstants.CONNECTION_RETRY_DELAY_MS)
        }
        
        Result.Error(
            ConnectionException("All connection methods failed"),
            "Could not connect to ${device.name}"
        )
    }
    
    override suspend fun reconnect(
        sessionId: SessionId,
        device: Device
    ): Result<Transport> = withContext(dispatcherProvider.io) {
        Timber.d("Attempting reconnection for session $sessionId")
        
        // Close old transport if exists
        activeTransports[sessionId]?.disconnect()
        
        // Retry with exponential backoff
        var delayMs = 500L
        repeat(3) { attempt ->
            val result = establishConnection(sessionId, device)
            if (result.isSuccess) {
                return@withContext result
            }
            
            if (attempt < 2) {
                delay(delayMs)
                delayMs *= 2 // Exponential backoff
            }
        }
        
        Result.Error(
            ConnectionException("Reconnection failed after 3 attempts"),
            "Could not reconnect to ${device.name}"
        )
    }
    
    override fun getTransport(sessionId: SessionId): Transport? {
        return activeTransports[sessionId]
    }
    
    override suspend fun closeConnection(sessionId: SessionId) {
        withContext(dispatcherProvider.io) {
            activeTransports[sessionId]?.let { transport ->
                transport.disconnect()
                activeTransports.remove(sessionId)
                updateConnectionState(sessionId, ConnectionState.DISCONNECTED)
            }
        }
    }
    
    override fun getConnectionState(sessionId: SessionId): StateFlow<ConnectionState> {
        // Return a state flow for this specific session
        return MutableStateFlow(
            _connectionStates.value[sessionId] ?: ConnectionState.DISCONNECTED
        ).asStateFlow()
    }
    
    private suspend fun attemptConnection(
        sessionId: SessionId,
        device: Device,
        method: ConnectionMethod
    ): Result<Transport> {
        return try {
            val transport = when (method) {
                ConnectionMethod.LAN -> createLanTransport(device)
                ConnectionMethod.WIFI_DIRECT -> createWifiDirectTransport(device)
                ConnectionMethod.HOTSPOT_SENDER -> createHotspotTransport(asSender = true)
                ConnectionMethod.HOTSPOT_RECEIVER -> createHotspotTransport(asSender = false)
                else -> throw IllegalArgumentException("Unsupported method: $method")
            }
            
            // Attempt to connect
            withTimeout(SendraConstants.CONNECTION_TIMEOUT_MS) {
                transport.connect(
                    Endpoint(
                        host = device.connectionInfo.ipAddress,
                        port = device.connectionInfo.port
                    )
                )
            }
            
            // Store successful transport
            activeTransports[sessionId] = transport
            updateConnectionState(sessionId, ConnectionState.CONNECTED)
            
            Result.Success(transport)
            
        } catch (e: Exception) {
            Timber.e(e, "Connection attempt failed for method: $method")
            Result.Error(e, "Failed to connect via $method")
        }
    }
    
    private fun determineConnectionMethods(device: Device): List<ConnectionMethod> {
        return when (device.connectionInfo.connectionMethod) {
            ConnectionMethod.LAN -> listOf(ConnectionMethod.LAN, ConnectionMethod.WIFI_DIRECT)
            ConnectionMethod.WIFI_DIRECT -> listOf(ConnectionMethod.WIFI_DIRECT, ConnectionMethod.HOTSPOT_SENDER)
            else -> listOf(
                ConnectionMethod.LAN,
                ConnectionMethod.WIFI_DIRECT,
                ConnectionMethod.HOTSPOT_SENDER
            )
        }
    }
    
    private suspend fun createLanTransport(device: Device): LanTransport {
        return LanTransport(dispatcherProvider)
    }
    
    private suspend fun createWifiDirectTransport(device: Device): WifiDirectTransport {
        // Initialize WiFi Direct group if needed
        wifiDirectController.createGroup()
        return WifiDirectTransport(dispatcherProvider, wifiDirectController)
    }
    
    private suspend fun createHotspotTransport(asSender: Boolean): HotspotTransport {
        if (asSender) {
            hotspotController.startHotspot()
        } else {
            // Connect to sender's hotspot
            // This would require UI prompt for user to connect manually on some devices
        }
        return HotspotTransport(dispatcherProvider, hotspotController, asSender)
    }
    
    private fun updateConnectionState(sessionId: SessionId, state: ConnectionState) {
        _connectionStates.value = _connectionStates.value.toMutableMap().apply {
            put(sessionId, state)
        }
    }
    
    class ConnectionException(message: String) : Exception(message)
}

interface ConnectionManager {
    suspend fun establishConnection(
        sessionId: SessionId,
        device: Device,
        preferredMethod: ConnectionMethod? = null
    ): Result<Transport>
    
    suspend fun reconnect(sessionId: SessionId, device: Device): Result<Transport>
    fun getTransport(sessionId: SessionId): Transport?
    suspend fun closeConnection(sessionId: SessionId)
    fun getConnectionState(sessionId: SessionId): StateFlow<ConnectionState>
}

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    FAILED
}
