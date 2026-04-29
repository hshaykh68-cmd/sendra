package com.sendra.connection.controller

import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.lang.reflect.Method
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HotspotController @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val wifiManager: WifiManager? = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    private val connectivityManager: ConnectivityManager? = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    
    private var wasWifiEnabled = false
    private var hotspotActive = false
    
    fun startHotspot(ssid: String = "Sendra-${(1000..9999).random()}", password: String = generatePassword()): Result<HotspotInfo> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Android 8+ requires privileged permissions or Settings.ACTION_TETHER_SETTINGS
            // For Sendra, we use a local-only hotspot approach
            return startLocalOnlyHotspot(ssid, password)
        } else {
            // Pre-Android 8, use reflection (deprecated but functional)
            return startLegacyHotspot(ssid, password)
        }
    }
    
    private fun startLocalOnlyHotspot(ssid: String, password: String): Result<HotspotInfo> {
        // Android 8+ LocalOnlyHotspotConfiguration
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Note: LocalOnlyHotspot requires LOCATION permission
                // And can only be started via WifiManager.startLocalOnlyHotspot()
                // This requires the app to be foreground
                
                // Since starting LocalOnlyHotspot is complex and requires callbacks,
                // we return a failure here and handle it in the UI layer
                // or use a pre-existing connection
                Result.failure(IllegalStateException("LocalOnlyHotspot requires foreground UI interaction"))
            } else {
                Result.failure(IllegalStateException("LocalOnlyHotspot not available on this Android version"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    @Suppress("DEPRECATION")
    private fun startLegacyHotspot(ssid: String, password: String): Result<HotspotInfo> {
        return try {
            wifiManager ?: return Result.failure(IllegalStateException("WiFi manager not available"))
            
            // Save current WiFi state
            wasWifiEnabled = wifiManager.isWifiEnabled
            
            // Disable WiFi (required for hotspot on many devices)
            wifiManager.isWifiEnabled = false
            
            // Create hotspot configuration
            val wifiConfig = WifiConfiguration().apply {
                this.SSID = ssid
                this.preSharedKey = password
                this.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK)
            }
            
            // Use reflection to access hidden API
            val method: Method = wifiManager.javaClass.getMethod(
                "setWifiApEnabled",
                WifiConfiguration::class.java,
                Boolean::class.javaPrimitiveType
            )
            
            val enabled = method.invoke(wifiManager, wifiConfig, true) as? Boolean ?: false
            
            if (enabled) {
                hotspotActive = true
                Result.success(HotspotInfo(
                    ssid = ssid,
                    password = password,
                    ipAddress = getHotspotIpAddress() ?: "192.168.43.1" // Default Android hotspot IP
                ))
            } else {
                Result.failure(IllegalStateException("Failed to enable hotspot"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to start hotspot")
            Result.failure(e)
        }
    }
    
    fun stopHotspot() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Stop LocalOnlyHotspot
                // This is handled automatically when the app goes to background
            } else {
                // Stop legacy hotspot
                @Suppress("DEPRECATION")
                wifiManager?.let { wm ->
                    val method: Method = wm.javaClass.getMethod(
                        "setWifiApEnabled",
                        WifiConfiguration::class.java,
                        Boolean::class.javaPrimitiveType
                    )
                    method.invoke(wm, null, false)
                    
                    // Restore WiFi state
                    if (wasWifiEnabled) {
                        wm.isWifiEnabled = true
                    }
                }
            }
            hotspotActive = false
        } catch (e: Exception) {
            Timber.e(e, "Error stopping hotspot")
        }
    }
    
    fun isHotspotActive(): Boolean = hotspotActive
    
    fun getClientIpList(): List<String> {
        // Get list of connected clients
        // Requires reading ARP table or using DhcpInfo
        return try {
            val dhcpInfo = wifiManager?.dhcpInfo
            val dhcpLeaseParser = wifiManager?.javaClass?.getDeclaredMethod("getClientIp")
            // This is device-specific and may not work on all devices
            emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun getHotspotIpAddress(): String? {
        // Get the IP address of the hotspot interface
        return try {
            val dhcpInfo = wifiManager?.dhcpInfo
            val serverAddress = dhcpInfo?.serverAddress
            if (serverAddress != null) {
                // Convert from integer IP to string
                String.format(
                    "%d.%d.%d.%d",
                    (serverAddress and 0xFF),
                    (serverAddress shr 8 and 0xFF),
                    (serverAddress shr 16 and 0xFF),
                    (serverAddress shr 24 and 0xFF)
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }
    
    private fun generatePassword(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..8).map { chars.random() }.joinToString("")
    }
}

data class HotspotInfo(
    val ssid: String,
    val password: String,
    val ipAddress: String
)
