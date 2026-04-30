package com.sendra.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sendra.domain.repository.TransferRepository
import com.sendra.ui.model.UiTransferHistory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val transferRepository: TransferRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                // Collect from repository flow
                transferRepository.getTransferHistory().collect { sessions ->
                    val historyItems = sessions.map { session ->
                        UiTransferHistory(
                            id = session.id,
                            sessionId = session.id,
                            timestamp = session.createdAt,
                            direction = session.direction.name,
                            status = session.status.name,
                            deviceId = session.targetDevice.id,
                            deviceName = session.targetDevice.name,
                            totalBytes = session.totalBytes,
                            bytesTransferred = session.totalBytes, // Assume completed for history
                            fileCount = session.files.size
                        )
                    }
                    _uiState.update { 
                        it.copy(
                            historyItems = historyItems,
                            isLoading = false
                        ) 
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message
                    ) 
                }
            }
        }
    }

    fun onItemClicked(item: UiTransferHistory) {
        // Could navigate to detail view or show details in a bottom sheet
    }

    fun deleteHistoryItem(id: String) {
        viewModelScope.launch {
            try {
                transferRepository.deleteSession(id)
                _uiState.update { currentState ->
                    currentState.copy(
                        historyItems = currentState.historyItems.filter { it.id != id }
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            try {
                transferRepository.clearOldHistory(olderThanMillis = System.currentTimeMillis())
                _uiState.update { it.copy(historyItems = emptyList()) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun refresh() {
        loadHistory()
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

data class HistoryUiState(
    val historyItems: List<UiTransferHistory> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
