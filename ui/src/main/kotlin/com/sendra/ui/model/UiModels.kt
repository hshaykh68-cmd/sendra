package com.sendra.ui.model

/**
 * UI-specific model for transfer history items.
 * Decouples the UI layer from domain/data layer entities.
 */
data class UiTransferHistory(
    val id: String,
    val sessionId: String,
    val timestamp: Long,
    val direction: String, // "SEND" or "RECEIVE"
    val status: String, // "COMPLETED", "FAILED", "INTERRUPTED", etc.
    val deviceId: String,
    val deviceName: String,
    val totalBytes: Long,
    val bytesTransferred: Long,
    val fileCount: Int
)
