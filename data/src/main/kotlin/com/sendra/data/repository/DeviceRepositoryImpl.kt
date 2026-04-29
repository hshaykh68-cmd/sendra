package com.sendra.data.repository

import com.sendra.data.local.database.DeviceDao
import com.sendra.data.local.database.DeviceEntity
import com.sendra.domain.model.*
import com.sendra.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceRepositoryImpl @Inject constructor(
    private val deviceDao: DeviceDao
) : DeviceRepository {
    
    override suspend fun saveDiscoveredDevice(device: Device) {
        deviceDao.insert(device.toEntity())
    }
    
    override suspend fun updateDeviceSignalStrength(deviceId: DeviceId, rssi: Int) {
        deviceDao.updateSignal(deviceId, rssi)
    }
    
    override suspend fun markDeviceOffline(deviceId: DeviceId) {
        // Keep in DB but will be filtered out by timestamp checks
    }
    
    override suspend fun removeOfflineDevices(timeoutMs: Long) {
        val cutoff = System.currentTimeMillis() - timeoutMs
        deviceDao.deleteStale(cutoff)
    }
    
    override fun getDiscoveredDevices(): Flow<List<Device>> {
        return deviceDao.getAllActive().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun getDeviceById(deviceId: DeviceId): Flow<Device?> {
        return deviceDao.getByIdFlow(deviceId).map { it?.toDomain() }
    }
    
    override suspend fun getDeviceSync(deviceId: DeviceId): Device? {
        return deviceDao.getById(deviceId)?.toDomain()
    }
    
    override suspend fun updateTrustStatus(deviceId: DeviceId, status: TrustStatus) {
        deviceDao.updateTrustStatus(deviceId, status.name)
    }
    
    override fun getTrustedDevices(): Flow<List<Device>> {
        return deviceDao.getTrusted().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override suspend fun isDeviceTrusted(deviceId: DeviceId): Boolean {
        return deviceDao.getById(deviceId)?.trustStatus == TrustStatus.TRUSTED.name
    }
    
    override suspend fun blockDevice(deviceId: DeviceId) {
        deviceDao.updateBlocked(deviceId, true)
    }
    
    override suspend fun unblockDevice(deviceId: DeviceId) {
        deviceDao.updateBlocked(deviceId, false)
    }
    
    override suspend fun recordConnection(deviceId: DeviceId, success: Boolean) {
        if (success) {
            deviceDao.recordConnection(deviceId)
        }
    }
    
    override suspend fun getConnectionHistory(deviceId: DeviceId): List<Long> {
        // Simplified - in real implementation, could have separate table for connection history
        return emptyList()
    }
    
    override suspend fun clearAllDevices() {
        deviceDao.deleteAll()
    }
    
    // Mappers
    private fun Device.toEntity(): DeviceEntity {
        return DeviceEntity(
            id = id,
            name = name,
            type = type.name,
            ipAddress = connectionInfo.ipAddress,
            port = connectionInfo.port,
            connectionMethod = connectionInfo.connectionMethod.name,
            capabilities = capabilities.toBitmask(),
            trustStatus = trustStatus.name,
            rssi = signalStrength?.rssi,
            lastSeen = lastSeen,
            lastConnected = null,
            connectionCount = 0,
            isBlocked = trustStatus == TrustStatus.BLOCKED
        )
    }
    
    private fun DeviceEntity.toDomain(): Device {
        return Device(
            id = id,
            name = name,
            type = DeviceType.valueOf(type),
            capabilities = capabilitiesFromBitmask(capabilities),
            connectionInfo = ConnectionInfo(
                ipAddress = ipAddress,
                port = port,
                connectionMethod = ConnectionMethod.valueOf(connectionMethod)
            ),
            signalStrength = rssi?.let {
                SignalStrength(
                    rssi = it,
                    level = normalizeRssi(it)
                )
            },
            trustStatus = if (isBlocked) TrustStatus.BLOCKED else TrustStatus.valueOf(trustStatus),
            lastSeen = lastSeen
        )
    }

    private fun DeviceCapabilities.toBitmask(): Int {
        var mask = 0
        if (canSend) mask = mask or 0x01
        if (canReceive) mask = mask or 0x02
        if (supportsWeb) mask = mask or 0x04
        if (supportsResume) mask = mask or 0x08
        return mask
    }

    private fun capabilitiesFromBitmask(mask: Int): DeviceCapabilities {
        return DeviceCapabilities(
            canSend = mask and 0x01 != 0,
            canReceive = mask and 0x02 != 0,
            supportsWeb = mask and 0x04 != 0,
            supportsResume = mask and 0x08 != 0
        )
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
