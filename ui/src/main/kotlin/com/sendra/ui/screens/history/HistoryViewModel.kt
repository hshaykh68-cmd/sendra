package com.sendra.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sendra.data.local.database.HistoryDao
import com.sendra.data.local.database.TransferHistoryEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyDao: HistoryDao
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
                val history = historyDao.getRecentHistory(limit = 100)
                _uiState.update { 
                    it.copy(
                        historyItems = history,
                        isLoading = false
                    ) 
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

    fun onItemClicked(item: TransferHistoryEntity) {
        // Could navigate to detail view or show details in a bottom sheet
        // For now, just log or show a snackbar
    }

    fun deleteHistoryItem(id: String) {
        viewModelScope.launch {
            try {
                // Update the list locally (individual delete would require DAO update)
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
                // Delete old history (older than now, which effectively clears all)
                historyDao.deleteOldHistory(cutoff = System.currentTimeMillis())
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
    val historyItems: List<TransferHistoryEntity> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
