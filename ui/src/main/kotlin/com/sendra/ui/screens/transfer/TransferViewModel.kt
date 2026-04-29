package com.sendra.ui.screens.transfer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sendra.core.dispatcher.DispatcherProvider
import com.sendra.core.result.Result
import com.sendra.domain.model.*
import com.sendra.transfer.manager.TransferManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class TransferViewModel @Inject constructor(
    private val transferManager: TransferManager,
    private val dispatcherProvider: DispatcherProvider,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val sessionId: String = savedStateHandle["sessionId"]!!
    
    private val _uiState = MutableStateFlow(TransferUiState())
    val uiState: StateFlow<TransferUiState> = _uiState.asStateFlow()
    
    private val _events = MutableSharedFlow<TransferEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<TransferEvent> = _events.asSharedFlow()
    
    init {
        collectTransferState()
    }
    
    private fun collectTransferState() {
        viewModelScope.launch {
            transferManager.getTransferState(sessionId)
                .flowOn(dispatcherProvider.default)
                .collect { state ->
                    updateUiState(state)
                }
        }
    }
    
    private fun updateUiState(state: TransferState) {
        _uiState.update { current ->
            when (state.status) {
                TransferStatus.PREPARING -> current.copy(
                    status = TransferScreenStatus.PREPARING,
                    isLoading = true
                )
                
                TransferStatus.IN_PROGRESS -> {
                    val progress = state.progress
                    current.copy(
                        status = TransferScreenStatus.ACTIVE,
                        isLoading = false,
                        progress = progress?.percent ?: 0,
                        bytesTransferred = progress?.bytesTransferred ?: 0,
                        totalBytes = progress?.totalBytes ?: 0,
                        currentFileIndex = progress?.currentFileIndex ?: 0,
                        totalFiles = progress?.totalFiles ?: 1,
                        speedText = progress?.formattedSpeed ?: "",
                        etaText = progress?.formattedEta ?: "",
                        isPaused = false
                    )
                }
                
                TransferStatus.PAUSED -> current.copy(
                    status = TransferScreenStatus.PAUSED,
                    isPaused = true
                )
                
                TransferStatus.RECONNECTING -> current.copy(
                    status = TransferScreenStatus.RECONNECTING,
                    isLoading = true,
                    statusMessage = "Reconnecting..."
                )
                
                TransferStatus.COMPLETED -> current.copy(
                    status = TransferScreenStatus.COMPLETED,
                    isLoading = false,
                    progress = 100,
                    canNavigateBack = true
                ).also {
                    viewModelScope.launch {
                        _events.emit(TransferEvent.TransferComplete)
                    }
                }
                
                TransferStatus.FAILED -> current.copy(
                    status = TransferScreenStatus.FAILED,
                    isLoading = false,
                    errorMessage = state.error?.message ?: "Transfer failed",
                    canResume = state.canResume
                )
                
                TransferStatus.CANCELLED -> current.copy(
                    status = TransferScreenStatus.CANCELLED,
                    isLoading = false,
                    canNavigateBack = true
                )
                
                else -> current
            }
        }
    }
    
    fun onPauseClicked() {
        viewModelScope.launch {
            transferManager.pauseTransfer(sessionId)
        }
    }
    
    fun onResumeClicked() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, statusMessage = "Resuming...") }
            
            val success = transferManager.resumeTransfer(sessionId)
            if (!success) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Could not resume transfer"
                    )
                }
            }
        }
    }
    
    fun onCancelClicked() {
        viewModelScope.launch {
            transferManager.cancelTransfer(sessionId)
            _events.emit(TransferEvent.NavigateBack)
        }
    }
    
    fun onRetryClicked() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, statusMessage = "Retrying...") }
            
            val success = transferManager.resumeTransfer(sessionId)
            if (!success) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Retry failed. Please try again."
                    )
                }
            }
        }
    }
    
    fun onDoneClicked() {
        viewModelScope.launch {
            _events.emit(TransferEvent.NavigateBack)
        }
    }
}

data class TransferUiState(
    val status: TransferScreenStatus = TransferScreenStatus.PREPARING,
    val isLoading: Boolean = true,
    val progress: Int = 0,
    val bytesTransferred: Long = 0,
    val totalBytes: Long = 0,
    val currentFileIndex: Int = 0,
    val totalFiles: Int = 1,
    val speedText: String = "",
    val etaText: String = "",
    val isPaused: Boolean = false,
    val canResume: Boolean = false,
    val canNavigateBack: Boolean = false,
    val errorMessage: String? = null,
    val statusMessage: String? = null
)

enum class TransferScreenStatus {
    PREPARING,
    ACTIVE,
    PAUSED,
    RECONNECTING,
    COMPLETED,
    FAILED,
    CANCELLED
}

sealed class TransferEvent {
    object TransferComplete : TransferEvent()
    object NavigateBack : TransferEvent()
    data class ShowSnackbar(val message: String) : TransferEvent()
}
