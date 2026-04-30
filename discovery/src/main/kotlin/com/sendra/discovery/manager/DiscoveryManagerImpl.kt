package com.sendra.discovery.manager

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.sendra.core.constants.SendraConstants
import com.sendra.core.dispatcher.DispatcherProvider
import com.sendra.core.result.Result
import com.sendra.discovery.cache.DeviceCache
import com.sendra.discovery.model.DiscoveryEvent
import com.sendra.domain.model.*
import com.sendra.domain.repository.DeviceRepository
import com.sendra.domain.usecase.discovery.DiscoveryManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.net.InetAddress
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiscoveryManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceRepository: DeviceRepository,
    private val dispatcherProvider: DispatcherProvider,
    private val deviceCache: DeviceCache
) : DiscoveryManager {
    
    private val nsdManager: NsdManager? = context.getSystemService(Context.NSD_SERVICE) as? NsdManager
    
    private val _discoveryEvents = MutableSharedFlow<DiscoveryEvent>(
        extraBufferCapacity = 64,
        replay = 1
    )
    override fun getDiscoveredDevices(): Flow<List<Device>> = deviceCache.devices
    
    private var discoveryJob: Job? = null
    private var isActive = false
    
    private val discoveredServices = ConcurrentHashMap<String, NsdServiceInfo>()
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    
    override fun startDiscovery() {
        if (isActive) return
        isActive = true
        
        Timber.d("Starting discovery")
        
        discoveryJob = CoroutineScope(dispatcherProvider.io).launch {
            // Start NSD registration (announce ourselves)
            registerService()
            
            // Start NSD discovery (find others)
            startNsdDiscovery()
            
            // Periodic cleanup of stale devices
            launch {
                while (isActive) {
                    delay(SendraConstants.DEVICE_OFFLINE_TIMEOUT_MS)
                    cleanupStaleDevices()
                }
            }
        }
    }
    
    override fun stopDiscovery() {
        isActive = false
        discoveryJob?.cancel()
        
        nsdManager?.apply {
            registrationListener?.let { unregisterService(it) }
            discoveryListener?.let { stopServiceDiscovery(it) }
        }
        
        discoveredServices.clear()
        Timber.d("Discovery stopped")
    }
    
    override fun isDiscoveryActive(): Boolean = isActive
    
    override suspend fun refreshDiscovery() {
        if (!isActive) return
        
        withContext(dispatcherProvider.io) {
            // Stop and restart to refresh
            stopDiscovery()
            delay(500)
            startDiscovery()
        }
    }
    
    private fun registerService() {
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = "${getDeviceName()}_${UUID.randomUUID().toString().take(8)}"
            serviceType = SendraConstants.DISCOVERY_SERVICE_TYPE
            port = SendraConstants.DEFAULT_TRANSFER_PORT
            
            // Set attributes
            setAttribute("device_id", getDeviceId())
            setAttribute("device_type", "PHONE") // Could detect tablet
            setAttribute("protocol_ver", "1")
            setAttribute("capabilities", "send,receive,resume")
        }
        
        registrationListener = object : NsdManager.RegistrationListener {
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                Timber.e("NSD registration failed: $errorCode")
            }
            
            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                Timber.e("NSD unregistration failed: $errorCode")
            }
            
            override fun onServiceRegistered(serviceInfo: NsdServiceInfo?) {
                Timber.d("NSD service registered: ${serviceInfo?.serviceName}")
            }
            
            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo?) {
                Timber.d("NSD service unregistered")
            }
        }
        
        try {
            nsdManager?.registerService(
                serviceInfo,
                NsdManager.PROTOCOL_DNS_SD,
                registrationListener
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to register NSD service")
        }
    }
    
    private fun startNsdDiscovery() {
        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String?) {
                Timber.d("NSD discovery started: $regType")
            }
            
            override fun onDiscoveryStopped(serviceType: String?) {
                Timber.d("NSD discovery stopped: $serviceType")
            }
            
            override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
                Timber.e("NSD discovery start failed: $errorCode")
            }
            
            override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {
                Timber.e("NSD discovery stop failed: $errorCode")
            }
            
            override fun onServiceFound(serviceInfo: NsdServiceInfo?) {
                serviceInfo ?: return
                
                // Don't resolve self
                if (serviceInfo.serviceName.contains(getDeviceId().take(8))) return
                
                discoveredServices[serviceInfo.serviceName] = serviceInfo
                
                // Resolve service to get details
                nsdManager?.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                        Timber.e("Service resolve failed: $errorCode")
                    }
                    
                    override fun onServiceResolved(resolvedInfo: NsdServiceInfo?) {
                        resolvedInfo?.let { info ->
                            handleDiscoveredService(info)
                        }
                    }
                })
            }
            
            override fun onServiceLost(serviceInfo: NsdServiceInfo?) {
                serviceInfo ?: return
                
                discoveredServices.remove(serviceInfo.serviceName)
                
                val deviceId = serviceInfo.attributes["device_id"]?.toString() ?: return
                
                CoroutineScope(dispatcherProvider.io).launch {
                    deviceCache.removeDevice(deviceId)
                    _discoveryEvents.emit(DiscoveryEvent.DeviceLost(deviceId))
                }
            }
        }
        
        try {
            nsdManager?.discoverServices(
                SendraConstants.DISCOVERY_SERVICE_TYPE,
                NsdManager.PROTOCOL_DNS_SD,
                discoveryListener
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to start NSD discovery")
        }
    }
    
    private fun handleDiscoveredService(serviceInfo: NsdServiceInfo) {
        val attributes = serviceInfo.attributes
        
        val deviceId = attributes["device_id"]?.toString() ?: UUID.randomUUID().toString()
        val deviceName = serviceInfo.serviceName.substringBefore("_")
        val deviceType = attributes["device_type"]?.toString() ?: "PHONE"
        val capabilities = attributes["capabilities"]?.toString() ?: "send,receive"
        
        val hostAddress = serviceInfo.host?.hostAddress
        val port = serviceInfo.port
        
        if (hostAddress == null) {
            Timber.w("Discovered service has no host address")
            return
        }
        
        val device = Device(
            id = deviceId,
            name = deviceName,
            type = DeviceType.valueOf(deviceType),
            capabilities = parseCapabilities(capabilities),
            connectionInfo = ConnectionInfo(
                ipAddress = hostAddress,
                port = port,
                connectionMethod = ConnectionMethod.LAN // NSD implies same network
            ),
            signalStrength = null, // Will be updated via RSSI if available
            trustStatus = TrustStatus.UNKNOWN,
            lastSeen = System.currentTimeMillis()
        )
        
        CoroutineScope(dispatcherProvider.io).launch {
            deviceCache.addOrUpdateDevice(device)
            deviceRepository.saveDiscoveredDevice(device)
            _discoveryEvents.emit(DiscoveryEvent.DeviceFound(device))
        }
    }
    
    private fun cleanupStaleDevices() {
        CoroutineScope(dispatcherProvider.io).launch {
            deviceRepository.removeOfflineDevices(SendraConstants.DEVICE_OFFLINE_TIMEOUT_MS)
            deviceCache.removeStaleDevices(SendraConstants.DEVICE_OFFLINE_TIMEOUT_MS)
        }
    }
    
    private fun parseCapabilities(capString: String): DeviceCapabilities {
        val caps = capString.split(",")
        return DeviceCapabilities(
            canSend = caps.contains("send"),
            canReceive = caps.contains("receive"),
            supportsWeb = caps.contains("web"),
            supportsResume = caps.contains("resume")
        )
    }
    
    private fun getDeviceName(): String {
        return android.os.Build.MODEL ?: "Android Device"
    }
    
    private fun getDeviceId(): String {
        // In production, use persistent UUID stored in DataStore
        return UUID.randomUUID().toString()
    }
}
