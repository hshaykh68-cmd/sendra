package com.sendra.domain.model

data class Device(
    val id: DeviceId,
    val name: String,
    val type: DeviceType,
    val capabilities: DeviceCapabilities,
    val connectionInfo: ConnectionInfo,
    val signalStrength: SignalStrength? = null,
    val trustStatus: TrustStatus = TrustStatus.UNKNOWN,
    val lastSeen: Long = System.currentTimeMillis()
) {
    val isTrusted: Boolean get() = trustStatus == TrustStatus.TRUSTED
    val canAcceptTransfers: Boolean get() = capabilities.canReceive
}

typealias DeviceId = String

enum class DeviceType {
    PHONE,
    TABLET,
    LAPTOP,
    DESKTOP,
    UNKNOWN
}

data class DeviceCapabilities(
    val canSend: Boolean = true,
    val canReceive: Boolean = true,
    val supportsWeb: Boolean = false,
    val supportsResume: Boolean = true,
    val protocolVersion: Int = 1
)

data class ConnectionInfo(
    val ipAddress: String,
    val port: Int,
    val connectionMethod: ConnectionMethod
)

enum class ConnectionMethod {
    LAN,
    WIFI_DIRECT,
    HOTSPOT_SENDER,
    HOTSPOT_RECEIVER,
    UNKNOWN
}

enum class TrustStatus {
    UNKNOWN,
    TRUSTED,
    BLOCKED
}

data class SignalStrength(
    val rssi: Int, // dBm, e.g., -50
    val level: Int // 0-4 normalized
) {
    val proximity: Proximity = when {
        rssi >= -40 -> Proximity.IMMEDIATE
        rssi >= -55 -> Proximity.NEAR
        rssi >= -70 -> Proximity.MID
        rssi >= -80 -> Proximity.FAR
        else -> Proximity.EDGE
    }
}

enum class Proximity {
    IMMEDIATE, // <1m
    NEAR,      // 1-3m
    MID,       // 3-10m
    FAR,       // 10-20m
    EDGE       // >20m, unreliable
}
