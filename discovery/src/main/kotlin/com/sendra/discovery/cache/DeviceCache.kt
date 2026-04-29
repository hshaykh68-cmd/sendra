package com.sendra.discovery.cache

import com.sendra.domain.model.Device
import com.sendra.domain.model.DeviceId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceCache @Inject constructor() {
    
    private val _devices = MutableStateFlow<List<Device>>(emptyList())
    val devices: StateFlow<List<Device>> = _devices.asStateFlow()
    
    private val deviceMap = ConcurrentHashMap<DeviceId, CachedDevice>()
    
    data class CachedDevice(
        val device: Device,
        val lastUpdateTime: Long = System.currentTimeMillis()
    )
    
    fun getDevices(): StateFlow<List<Device>> = devices
    
    fun addOrUpdateDevice(device: Device) {
        val existing = deviceMap[device.id]
        
        // Smooth signal strength if device existed
        val deviceSignalStrength = device.signalStrength
        val smoothedDevice = if (existing != null && deviceSignalStrength != null) {
            val existingRssi = existing.device.signalStrength?.rssi
            val newRssi = deviceSignalStrength.rssi

            if (existingRssi != null) {
                // 70/30 smoothing
                val smoothedRssi = (existingRssi * 0.7 + newRssi * 0.3).toInt()
                device.copy(
                    signalStrength = deviceSignalStrength.copy(
                        rssi = smoothedRssi,
                        level = normalizeRssi(smoothedRssi)
                    )
                )
            } else {
                device
            }
        } else {
            device
        }
        
        deviceMap[device.id] = CachedDevice(
            device = smoothedDevice,
            lastUpdateTime = System.currentTimeMillis()
        )
        
        updateDeviceFlow()
    }
    
    fun removeDevice(deviceId: DeviceId) {
        deviceMap.remove(deviceId)
        updateDeviceFlow()
    }
    
    fun removeStaleDevices(timeoutMs: Long) {
        val cutoff = System.currentTimeMillis() - timeoutMs
        val staleIds = deviceMap.filter { it.value.lastUpdateTime < cutoff }.keys
        
        staleIds.forEach { deviceMap.remove(it) }
        
        if (staleIds.isNotEmpty()) {
            updateDeviceFlow()
        }
    }
    
    fun getDevice(deviceId: DeviceId): Device? {
        return deviceMap[deviceId]?.device
    }
    
    fun updateSignalStrength(deviceId: DeviceId, rssi: Int) {
        deviceMap[deviceId]?.let { cached ->
            val smoothedRssi = cached.device.signalStrength?.rssi?.let { existing ->
                (existing * 0.7 + rssi * 0.3).toInt()
            } ?: rssi
            
            val updatedDevice = cached.device.copy(
                signalStrength = cached.device.signalStrength?.copy(
                    rssi = smoothedRssi,
                    level = normalizeRssi(smoothedRssi)
                ) ?: com.sendra.domain.model.SignalStrength(
                    rssi = smoothedRssi,
                    level = normalizeRssi(smoothedRssi)
                )
            )
            
            deviceMap[deviceId] = cached.copy(
                device = updatedDevice,
                lastUpdateTime = System.currentTimeMillis()
            )
            
            updateDeviceFlow()
        }
    }
    
    fun clear() {
        deviceMap.clear()
        updateDeviceFlow()
    }
    
    private fun updateDeviceFlow() {
        _devices.update {
            deviceMap.values
                .map { it.device }
                .sortedWith(
                    compareByDescending<Device> { it.signalStrength?.level ?: 0 }
                        .thenByDescending { it.lastSeen }
                )
        }
    }
    
    private fun normalizeRssi(rssi: Int): Int {
        return when {
            rssi >= -50 -> 4
            rssi >= -60 -> 3
            rssi >= -70 -> 2
            rssi >= -80 -> 1
            else -> 0
        }
    }
}
