package com.sendra.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sendra.domain.model.FileInfo
import com.sendra.domain.usecase.discovery.DiscoveryManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val discoveryManager: DiscoveryManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    init {
        startDiscovery()
    }
    
    private fun startDiscovery() {
        viewModelScope.launch {
            discoveryManager.startDiscovery()
            _uiState.update { it.copy(isDiscoveryActive = true) }
            
            // Collect nearby device count
            discoveryManager.getDiscoveredDevices().collect { devices ->
                _uiState.update { it.copy(nearbyDeviceCount = devices.size) }
            }
        }
    }
    
    fun onFilesSelected(files: List<FileInfo>) {
        _uiState.update { it.copy(selectedFiles = files) }
    }
    
    fun clearSelection() {
        _uiState.update { it.copy(selectedFiles = emptyList()) }
    }
    
    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            discoveryManager.stopDiscovery()
        }
    }
}

data class HomeUiState(
    val isDiscoveryActive: Boolean = false,
    val nearbyDeviceCount: Int = 0,
    val selectedFiles: List<FileInfo> = emptyList()
)
