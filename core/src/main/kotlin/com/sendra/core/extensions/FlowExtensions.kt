package com.sendra.core.extensions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.delay
import kotlin.time.Duration

fun <T> Flow<T>.throttleLatest(intervalMillis: Long): Flow<T> = flow {
    var lastEmissionTime = 0L
    var pendingValue: T? = null
    var hasPendingValue = false
    
    collect { value ->
        val currentTime = System.currentTimeMillis()
        val elapsed = currentTime - lastEmissionTime
        
        if (elapsed >= intervalMillis) {
            lastEmissionTime = currentTime
            emit(value)
            hasPendingValue = false
        } else {
            pendingValue = value
            hasPendingValue = true
            
            delay(intervalMillis - elapsed)
            if (hasPendingValue && pendingValue == value) {
                lastEmissionTime = System.currentTimeMillis()
                emit(value)
                hasPendingValue = false
            }
        }
    }
}

fun <T> Flow<T>.throttleFirst(intervalMillis: Long): Flow<T> = flow {
    var lastEmissionTime = 0L
    
    collect { value ->
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastEmissionTime >= intervalMillis) {
            lastEmissionTime = currentTime
            emit(value)
        }
    }
}

fun Flow<Float>.smoothFloat(smoothingFactor: Float = 0.7f): Flow<Float> = flow {
    var smoothedValue = 0f
    var firstValue = true
    
    collect { value ->
        smoothedValue = if (firstValue) {
            firstValue = false
            value
        } else {
            smoothedValue * smoothingFactor + value * (1 - smoothingFactor)
        }
        emit(smoothedValue)
    }
}
