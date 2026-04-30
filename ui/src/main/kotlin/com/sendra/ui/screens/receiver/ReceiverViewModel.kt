package com.sendra.ui.screens.receiver

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sendra.domain.usecase.discovery.StartDiscoveryUseCase
import com.sendra.domain.usecase.discovery.StopDiscoveryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReceiverViewModel @Inject constructor(
    private val startDiscoveryUseCase: StartDiscoveryUseCase,
    private val stopDiscoveryUseCase: StopDiscoveryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReceiverUiState())
    val uiState: StateFlow<ReceiverUiState> = _uiState.asStateFlow()

    init {
        loadDeviceInfo()
    }

    private fun loadDeviceInfo() {
        val deviceName = Build.MODEL ?: "Unknown Device"
        _uiState.update { it.copy(deviceName = deviceName) }
    }

    fun startListening() {
        viewModelScope.launch {
            _uiState.update { it.copy(isListening = true) }
            startDiscoveryUseCase()
            
            // TODO: Start server socket to listen for incoming connections
            // This would integrate with TransferManager to receive files
        }
    }

    fun toggleListening() {
        if (_uiState.value.isListening) {
            stopListening()
        } else {
            startListening()
        }
    }

    private fun stopListening() {
        viewModelScope.launch {
            _uiState.update { it.copy(isListening = false) }
            stopDiscoveryUseCase()
        }
    }

    fun acceptRequest() {
        viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    hasIncomingRequest = false,
                    isReceiving = true
                ) 
            }
            // TODO: Accept the transfer and start receiving
        }
    }

    fun declineRequest() {
        viewModelScope.launch {
            _uiState.update { it.copy(hasIncomingRequest = false) }
            // TODO: Send decline response to sender
        }
    }

    fun cancelReceive() {
        viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    isReceiving = false,
                    receiveProgress = 0,
                    bytesReceived = 0
                ) 
            }
            // TODO: Cancel the ongoing transfer
        }
    }

    // Called when an incoming request is detected
    fun onIncomingRequest(request: IncomingRequest) {
        _uiState.update { 
            it.copy(
                hasIncomingRequest = true,
                incomingRequest = request
            ) 
        }
    }

    // Update receive progress
    fun updateProgress(progress: Int, bytesReceived: Long) {
        _uiState.update { 
            it.copy(
                receiveProgress = progress,
                bytesReceived = bytesReceived
            ) 
        }
    }

    // Called when receive is complete
    fun onReceiveComplete() {
        _uiState.update { 
            it.copy(
                isReceiving = false,
                receiveProgress = 100,
                hasIncomingRequest = false
            ) 
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopListening()
    }
}

data class ReceiverUiState(
    val isListening: Boolean = false,
    val deviceName: String = "",
    val connectionMethod: String = "WiFi / LAN",
    val hasIncomingRequest: Boolean = false,
    val incomingRequest: IncomingRequest? = null,
    val isReceiving: Boolean = false,
    val receiveProgress: Int = 0,
    val currentFileName: String = "",
    val senderName: String = "",
    val bytesReceived: Long = 0,
    val totalBytes: Long = 0
)
