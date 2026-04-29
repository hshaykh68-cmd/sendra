package com.sendra.core.constants

object SendraConstants {
    // Discovery
    const val DISCOVERY_SERVICE_TYPE = "_sendra._tcp.local."
    const val DISCOVERY_PORT = 5353
    const val DISCOVERY_BROADCAST_INTERVAL_MS = 1000L
    const val DISCOVERY_BURST_COUNT = 5
    const val DISCOVERY_BURST_INTERVAL_MS = 100L
    
    // Connection
    const val DEFAULT_TRANSFER_PORT = 8080
    const val PORT_RANGE_START = 8080
    const val PORT_RANGE_END = 8090
    const val CONNECTION_TIMEOUT_MS = 2000L
    const val CONNECTION_RETRY_DELAY_MS = 200L
    const val MAX_CONNECTION_RETRIES = 3
    
    // Transfer
    const val DEFAULT_CHUNK_SIZE = 262144L // 256KB
    const val MAX_CHUNK_SIZE = 2097152L // 2MB
    const val MIN_CHUNK_SIZE = 65536L // 64KB
    const val PARALLEL_STREAMS = 3
    const val MAX_PARALLEL_STREAMS = 4
    const val CHUNK_TIMEOUT_MS = 5000L
    const val PROGRESS_UPDATE_INTERVAL_MS = 500L
    const val SPEED_UPDATE_INTERVAL_MS = 1000L
    const val ETA_UPDATE_INTERVAL_MS = 5000L
    
    // Resume
    const val RESUME_STATE_PERSIST_INTERVAL_MS = 5000L
    const val RESUME_WINDOW_MS = 86400000L // 24 hours
    const val MAX_RESUME_RETRY_DURATION_MS = 30000L // 30 seconds
    
    // Device Cache
    const val DEVICE_OFFLINE_TIMEOUT_MS = 6000L // 6 seconds
    const val DEVICE_STALE_TIMEOUT_MS = 3000L // 3 seconds
    const val MAX_TRACKED_DEVICES = 50
    const val MAX_UI_DEVICES = 6
    
    // Storage
    const val MAX_HISTORY_ENTRIES = 1000
    const val HISTORY_RETENTION_DAYS = 30
    const val MAX_THUMBNAIL_CACHE_SIZE = 50 * 1024 * 1024L // 50MB
    const val DEFAULT_RECEIVED_DIR = "Sendra"
    
    // Network
    const val MAX_CONCURRENT_CONNECTIONS = 12
    const val MAX_DOWNLOAD_STREAMS_PER_USER = 2
    const val SOCKET_BUFFER_SIZE = 65536 // 64KB
    const val KEEP_ALIVE_INTERVAL_MS = 5000L
    
    // Drop Zone
    const val DROP_ZONE_MAX_USERS = 8
    const val DROP_ZONE_MAX_DURATION_MS = 7200000L // 2 hours
    const val DROP_ZONE_IDLE_TIMEOUT_MS = 600000L // 10 minutes
    const val DROP_ZONE_SESSION_ID_LENGTH = 8
}
