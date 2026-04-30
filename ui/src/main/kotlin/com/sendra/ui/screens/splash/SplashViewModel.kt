package com.sendra.ui.screens.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor() : ViewModel() {
    
    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()
    
    init {
        startSplashSequence()
    }
    
    private fun startSplashSequence() {
        viewModelScope.launch {
            // Phase 1: Logo appears
            delay(200)
            _uiState.value = _uiState.value.copy(
                animationState = SplashAnimationState.LOGO_APPEAR
            )
            
            // Phase 2: Text reveal
            delay(600)
            _uiState.value = _uiState.value.copy(
                animationState = SplashAnimationState.TEXT_REVEAL
            )
            
            // Phase 3: Tagline fade
            delay(400)
            _uiState.value = _uiState.value.copy(
                animationState = SplashAnimationState.TAGLINE_FADE
            )
            
            // Phase 4: Start loading
            delay(200)
            _uiState.value = _uiState.value.copy(
                animationState = SplashAnimationState.LOADING_START
            )
            
            // Simulate loading progress
            simulateLoading()
            
            // Phase 5: Complete
            _uiState.value = _uiState.value.copy(
                isComplete = true
            )
        }
    }
    
    private suspend fun simulateLoading() {
        val steps = listOf(0.2f, 0.4f, 0.6f, 0.8f, 0.95f, 1.0f)
        
        steps.forEach { progress ->
            delay(300) // Simulate work being done
            _uiState.value = _uiState.value.copy(
                loadingProgress = progress
            )
        }
        
        // Hold at 100% briefly
        delay(200)
    }
}

data class SplashUiState(
    val animationState: SplashAnimationState = SplashAnimationState.IDLE,
    val loadingProgress: Float = 0f,
    val isComplete: Boolean = false
)

enum class SplashAnimationState {
    IDLE,           // Initial state
    LOGO_APPEAR,    // Logo scales in
    TEXT_REVEAL,    // Brand name reveals
    TAGLINE_FADE,   // Tagline appears
    LOADING_START,  // Progress bar starts
    COMPLETE        // All done
}
