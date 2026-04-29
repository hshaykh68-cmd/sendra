package com.sendra.domain.repository

import com.sendra.domain.model.Device
import com.sendra.domain.model.DeviceId
import com.sendra.domain.model.TrustStatus
import kotlinx.coroutines.flow.Flow

interface DeviceRepository {
    // Discovery
    suspend fun saveDiscoveredDevice(device: Device)
    suspend fun updateDeviceSignalStrength(deviceId: DeviceId, rssi: Int)
    suspend fun markDeviceOffline(deviceId: DeviceId)
    suspend fun removeOfflineDevices(timeoutMs: Long)
    
    // Queries
    fun getDiscoveredDevices(): Flow<List<Device>>
    fun getDeviceById(deviceId: DeviceId): Flow<Device?>
    suspend fun getDeviceSync(deviceId: DeviceId): Device?
    
    // Trust management
    suspend fun updateTrustStatus(deviceId: DeviceId, status: TrustStatus)
    fun getTrustedDevices(): Flow<List<Device>>
    suspend fun isDeviceTrusted(deviceId: DeviceId): Boolean
    suspend fun blockDevice(deviceId: DeviceId)
    suspend fun unblockDevice(deviceId: DeviceId)
    
    // History
    suspend fun recordConnection(deviceId: DeviceId, success: Boolean)
    suspend fun getConnectionHistory(deviceId: DeviceId): List<Long>
    
    // Cleanup
    suspend fun clearAllDevices()
}
