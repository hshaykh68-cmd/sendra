package com.sendra.discovery.model

import com.sendra.domain.model.Device
import com.sendra.domain.model.DeviceId

sealed class DiscoveryEvent {
    data class DeviceFound(val device: Device) : DiscoveryEvent()
    data class DeviceLost(val deviceId: DeviceId) : DiscoveryEvent()
    data class DeviceUpdated(val device: Device) : DiscoveryEvent()
    object DiscoveryStarted : DiscoveryEvent()
    object DiscoveryStopped : DiscoveryEvent()
    data class DiscoveryError(val error: Throwable) : DiscoveryEvent()
}
