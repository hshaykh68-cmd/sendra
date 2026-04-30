package com.sendra.ui.screens.settings

import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val prefs = context.getSharedPreferences("sendra_settings", Context.MODE_PRIVATE)
            
            _uiState.update { state ->
                state.copy(
                    deviceName = prefs.getString("device_name", Build.MODEL) ?: Build.MODEL,
                    autoAcceptFromTrusted = prefs.getBoolean("auto_accept_trusted", false),
                    downloadLocation = prefs.getString("download_location", "/storage/emulated/0/Download/Sendra") ?: "/storage/emulated/0/Download/Sendra",
                    chunkSize = prefs.getString("chunk_size", "256 KB") ?: "256 KB",
                    connectionMethod = prefs.getString("connection_method", "Auto (LAN/WiFi Direct)") ?: "Auto (LAN/WiFi Direct)",
                    isDiscoveryEnabled = prefs.getBoolean("discovery_enabled", true),
                    themeMode = prefs.getString("theme", "System default") ?: "System default",
                    useDynamicColors = prefs.getBoolean("dynamic_colors", true),
                    showTransferNotifications = prefs.getBoolean("show_notifications", true),
                    enableVibration = prefs.getBoolean("enable_vibration", true),
                    appVersion = getAppVersion()
                )
            }
        }
    }

    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "${packageInfo.versionName} (${packageInfo.longVersionCode})"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    private fun saveSetting(key: String, value: Any) {
        viewModelScope.launch {
            val prefs = context.getSharedPreferences("sendra_settings", Context.MODE_PRIVATE)
            with(prefs.edit()) {
                when (value) {
                    is String -> putString(key, value)
                    is Boolean -> putBoolean(key, value)
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is Float -> putFloat(key, value)
                }
                apply()
            }
        }
    }

    // Device name
    fun updateDeviceName() {
        // TODO: Show dialog to edit device name
        // For now, just update with a timestamp to simulate
        val newName = "Sendra-${System.currentTimeMillis() % 10000}"
        _uiState.update { it.copy(deviceName = newName) }
        saveSetting("device_name", newName)
    }

    // Auto-accept
    fun setAutoAcceptFromTrusted(enabled: Boolean) {
        _uiState.update { it.copy(autoAcceptFromTrusted = enabled) }
        saveSetting("auto_accept_trusted", enabled)
    }

    // Download location
    fun changeDownloadLocation() {
        // TODO: Show folder picker
    }

    // Chunk size
    fun changeChunkSize() {
        // TODO: Show selection dialog with options: 64KB, 128KB, 256KB, 512KB, 1MB
        val sizes = listOf("64 KB", "128 KB", "256 KB", "512 KB", "1 MB")
        val currentIndex = sizes.indexOf(_uiState.value.chunkSize)
        val nextSize = sizes.getOrElse(currentIndex + 1) { sizes.first() }
        _uiState.update { it.copy(chunkSize = nextSize) }
        saveSetting("chunk_size", nextSize)
    }

    // Connection method
    fun changeConnectionMethod() {
        // TODO: Show selection dialog
        val methods = listOf(
            "Auto (LAN/WiFi Direct)",
            "LAN only",
            "WiFi Direct only",
            "Hotspot"
        )
        val currentIndex = methods.indexOf(_uiState.value.connectionMethod)
        val nextMethod = methods.getOrElse(currentIndex + 1) { methods.first() }
        _uiState.update { it.copy(connectionMethod = nextMethod) }
        saveSetting("connection_method", nextMethod)
    }

    // Discovery enabled
    fun setDiscoveryEnabled(enabled: Boolean) {
        _uiState.update { it.copy(isDiscoveryEnabled = enabled) }
        saveSetting("discovery_enabled", enabled)
    }

    // Theme
    fun changeTheme() {
        val themes = listOf("System default", "Light", "Dark")
        val currentIndex = themes.indexOf(_uiState.value.themeMode)
        val nextTheme = themes.getOrElse(currentIndex + 1) { themes.first() }
        _uiState.update { it.copy(themeMode = nextTheme) }
        saveSetting("theme", nextTheme)
    }

    // Dynamic colors
    fun setDynamicColors(enabled: Boolean) {
        _uiState.update { it.copy(useDynamicColors = enabled) }
        saveSetting("dynamic_colors", enabled)
    }

    // Notifications
    fun setShowTransferNotifications(enabled: Boolean) {
        _uiState.update { it.copy(showTransferNotifications = enabled) }
        saveSetting("show_notifications", enabled)
    }

    // Vibration
    fun setEnableVibration(enabled: Boolean) {
        _uiState.update { it.copy(enableVibration = enabled) }
        saveSetting("enable_vibration", enabled)
    }

    // About
    fun openPrivacyPolicy() {
        // TODO: Open privacy policy URL
    }

    fun openHelp() {
        // TODO: Open help page or show help dialog
    }
}

data class SettingsUiState(
    val deviceName: String = "",
    val autoAcceptFromTrusted: Boolean = false,
    val downloadLocation: String = "",
    val chunkSize: String = "256 KB",
    val connectionMethod: String = "Auto (LAN/WiFi Direct)",
    val isDiscoveryEnabled: Boolean = true,
    val themeMode: String = "System default",
    val useDynamicColors: Boolean = true,
    val showTransferNotifications: Boolean = true,
    val enableVibration: Boolean = true,
    val appVersion: String = "1.0.0"
)
