package com.sendra.connection.transport

import com.sendra.core.result.Result
import com.sendra.domain.model.ChunkId
import com.sendra.domain.model.FileChunk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface Transport {
    val controlChannel: ControlChannel
    val dataChannels: List<DataChannel>
    val isConnected: Boolean
    val connectionMethod: com.sendra.domain.model.ConnectionMethod
    
    suspend fun connect(endpoint: Endpoint): Result<Unit>
    suspend fun disconnect()
    fun connectionState(): StateFlow<TransportConnectionState>
}

interface ControlChannel {
    suspend fun send(command: ControlCommand): Result<Unit>
    fun receive(): Flow<ControlEvent>
    suspend fun sendHandshake(sessionId: String, capabilities: List<String>): Result<HandshakeResponse>
}

interface DataChannel {
    suspend fun sendChunk(chunk: FileChunk): Result<Unit>
    suspend fun receiveChunk(): Result<FileChunk>
    suspend fun sendAck(chunkId: ChunkId): Result<Unit>
    suspend fun waitForAck(chunkId: ChunkId, timeoutMs: Long): Result<Unit>
    fun isOpen(): Boolean
}

data class Endpoint(
    val host: String,
    val port: Int
)

sealed class ControlCommand {
    data class StartTransfer(val sessionId: String, val fileManifest: List<FileInfo>) : ControlCommand()
    data class PauseTransfer(val sessionId: String) : ControlCommand()
    data class ResumeTransfer(val sessionId: String) : ControlCommand()
    data class CancelTransfer(val sessionId: String) : ControlCommand()
    data class ChunkAck(val chunkId: ChunkId, val status: AckStatus) : ControlCommand()
    object Ping : ControlCommand()
}

sealed class ControlEvent {
    data class TransferAccepted(val sessionId: String, val receivePort: Int) : ControlEvent()
    data class TransferRejected(val sessionId: String, val reason: String) : ControlEvent()
    data class TransferPaused(val sessionId: String) : ControlEvent()
    data class TransferResumed(val sessionId: String) : ControlEvent()
    data class TransferCancelled(val sessionId: String) : ControlEvent()
    data class Error(val message: String) : ControlEvent()
    object Pong : ControlEvent()
}

data class HandshakeResponse(
    val accepted: Boolean,
    val protocolVersion: Int,
    val capabilities: List<String>,
    val receiveBufferSize: Int
)

data class FileInfo(
    val name: String,
    val size: Long,
    val mimeType: String,
    val hash: String?
)

enum class AckStatus {
    SUCCESS,
    CHECKSUM_MISMATCH,
    STORAGE_FULL,
    UNKNOWN_ERROR
}

enum class TransportConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}
