package com.sendra.ui.screens.radar

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sendra.core.dispatcher.DispatcherProvider
import com.sendra.core.result.Result
import com.sendra.domain.model.Device
import com.sendra.domain.model.FileInfo
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import com.sendra.domain.model.TransferSession
import com.sendra.domain.usecase.discovery.DiscoveryManager
import com.sendra.domain.usecase.discovery.StartDiscoveryUseCase
import com.sendra.domain.usecase.discovery.StopDiscoveryUseCase
import com.sendra.transfer.manager.TransferManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.util.Log
import javax.inject.Inject
import kotlin.math.cos
import kotlin.math.sin

@HiltViewModel
class RadarViewModel @Inject constructor(
    private val startDiscovery: StartDiscoveryUseCase,
    private val stopDiscovery: StopDiscoveryUseCase,
    private val discoveryManager: DiscoveryManager,
    private val transferManager: TransferManager,
    private val dispatcherProvider: DispatcherProvider,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val selectedFiles: List<FileInfo> = savedStateHandle.get<String>("files")?.let { json ->
        try {
            Json.decodeFromString<List<FileInfo>>(json)
        } catch (e: Exception) {
            emptyList()
        }
    } ?: emptyList()
    
    private val _uiState = MutableStateFlow(RadarUiState(
        selectedFiles = selectedFiles,
        totalSize = selectedFiles.sumOf { it.size }
    ))
    val uiState: StateFlow<RadarUiState> = _uiState.asStateFlow()
    
    private val _events = MutableSharedFlow<RadarEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<RadarEvent> = _events.asSharedFlow()
    
    private val devicePositions = mutableMapOf<String, PolarCoordinate>()
    
    init {
        startDiscoverySession()
        collectDiscoveredDevices()
    }
    
    private fun startDiscoverySession() {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true) }
            startDiscovery()
        }
    }
    
    private fun collectDiscoveredDevices() {
        viewModelScope.launch {
            discoveryManager.getDiscoveredDevices()
                .map { devices ->
                    // Calculate positions and apply smoothing
                    devices.map { device ->
                        DeviceNode(
                            device = device,
                            position = calculatePosition(device),
                            visualState = determineVisualState(device)
                        )
                    }
                }
                .flowOn(dispatcherProvider.default)
                .collect { nodes ->
                    _uiState.update { state ->
                        state.copy(
                            devices = nodes,
                            deviceCount = nodes.size
                        )
                    }
                }
        }
    }
    
    private fun calculatePosition(device: Device): PolarCoordinate {
        val existing = devicePositions[device.id]
        
        return if (existing != null) {
            // 70/30 smoothing for position stability
            val signalBasedDistance = signalToDistance(device.signalStrength?.rssi ?: -70)
            val smoothedDistance = existing.distance * 0.7 + signalBasedDistance * 0.3
            
            // Keep same angle, update distance
            existing.copy(distance = smoothedDistance.coerceIn(0.15, 0.85))
        } else {
            // New device: assign random angle, distance based on signal
            val angle = Math.random() * 2 * Math.PI
            val distance = signalToDistance(device.signalStrength?.rssi ?: -70)
            
            PolarCoordinate(
                angle = angle,
                distance = distance.coerceIn(0.15, 0.85)
            ).also {
                devicePositions[device.id] = it
            }
        }
    }
    
    private fun signalToDistance(rssi: Int): Double {
        // Convert RSSI to relative distance (0-1, where 0 is center, 1 is edge)
        return when {
            rssi >= -40 -> 0.15 // Very close
            rssi >= -55 -> 0.35
            rssi >= -70 -> 0.55
            rssi >= -80 -> 0.75
            else -> 0.90 // Edge
        }
    }
    
    private fun determineVisualState(device: Device): DeviceVisualState {
        return when {
            device.isTrusted -> DeviceVisualState.TRUSTED
            device.signalStrength?.proximity == com.sendra.domain.model.Proximity.IMMEDIATE -> 
                DeviceVisualState.PROMINENT
            else -> DeviceVisualState.NORMAL
        }
    }
    
    fun onDeviceSelected(device: Device) {
        viewModelScope.launch {
            if (_uiState.value.isTransferring) return@launch
            
            _uiState.update { it.copy(isTransferring = true, selectedDevice = device) }
            
            val result = transferManager.initiateTransfer(
                files = _uiState.value.selectedFiles,
                targetDevice = device
            )
            
            when (result) {
                is Result.Success -> {
                    _events.emit(RadarEvent.NavigateToTransfer(result.data.id))
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isTransferring = false) }
                    _events.emit(RadarEvent.ShowError(result.message ?: "Transfer failed"))
                }
                is Result.Loading -> { }
            }
        }
    }
    
    fun onStopDiscovery() {
        viewModelScope.launch {
            stopDiscovery()
            _uiState.update { it.copy(isScanning = false) }
        }
    }
    
    fun onRefreshDiscovery() {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true) }
            discoveryManager.refreshDiscovery()
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            stopDiscovery()
        }
    }
}

data class RadarUiState(
    val devices: List<DeviceNode> = emptyList(),
    val deviceCount: Int = 0,
    val selectedFiles: List<FileInfo> = emptyList(),
    val totalSize: Long = 0,
    val isScanning: Boolean = true,
    val isTransferring: Boolean = false,
    val selectedDevice: Device? = null
)

data class DeviceNode(
    val device: Device,
    val position: PolarCoordinate,
    val visualState: DeviceVisualState
)

data class PolarCoordinate(
    val angle: Double,
    val distance: Double
) {
    fun toCartesian(): Pair<Float, Float> {
        val x = (distance * cos(angle)).toFloat()
        val y = (distance * sin(angle)).toFloat()
        return Pair(x, y)
    }
}

enum class DeviceVisualState {
    NORMAL,
    PROMINENT,
    TRUSTED
}

sealed class RadarEvent {
    data class NavigateToTransfer(val sessionId: String) : RadarEvent()
    data class ShowError(val message: String) : RadarEvent()
    data class ShowToast(val message: String) : RadarEvent()
}
