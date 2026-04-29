package com.sendra.platform.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.sendra.app.MainActivity
import com.sendra.app.R
import com.sendra.app.SendraApplication
import com.sendra.transfer.manager.TransferManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class TransferForegroundService : Service() {
    
    @Inject
    lateinit var transferManager: TransferManager
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var wakeLock: PowerManager.WakeLock? = null
    
    private val binder = LocalBinder()
    
    inner class LocalBinder : Binder() {
        fun getService(): TransferForegroundService = this@TransferForegroundService
    }
    
    override fun onCreate() {
        super.onCreate()
        Timber.d("TransferForegroundService created")
        acquireWakeLock()
    }
    
    override fun onBind(intent: Intent?): IBinder = binder
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val sessionId = intent?.getStringExtra(EXTRA_SESSION_ID)
        
        when (action) {
            ACTION_START_TRANSFER -> {
                if (sessionId != null) {
                    startTransferMonitoring(sessionId)
                }
            }
            ACTION_PAUSE -> {
                sessionId?.let { transferManager.pauseTransfer(it) }
            }
            ACTION_RESUME -> {
                sessionId?.let { 
                    serviceScope.launch {
                        transferManager.resumeTransfer(it)
                    }
                }
            }
            ACTION_CANCEL -> {
                sessionId?.let { 
                    serviceScope.launch {
                        transferManager.cancelTransfer(it)
                        stopSelf()
                    }
                }
            }
            ACTION_STOP_SERVICE -> {
                stopSelf()
            }
        }
        
        return START_NOT_STICKY
    }
    
    private fun startTransferMonitoring(sessionId: String) {
        // Start as foreground service immediately
        val notification = createTransferNotification(
            title = "File Transfer",
            content = "Preparing to transfer...",
            progress = 0
        )
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        
        // Monitor transfer state
        serviceScope.launch {
            transferManager.getTransferState(sessionId)
                .collectLatest { state ->
                    when (state.status) {
                        com.sendra.domain.model.TransferStatus.IN_PROGRESS -> {
                            state.progress?.let { progress ->
                                updateNotification(
                                    title = "Transferring...",
                                    content = "${progress.formattedSpeed} • ${progress.formattedEta}",
                                    progress = progress.percent
                                )
                            }
                        }
                        com.sendra.domain.model.TransferStatus.PAUSED -> {
                            updateNotification(
                                title = "Transfer Paused",
                                content = "Tap to resume",
                                progress = state.progress?.percent ?: 0,
                                showResumeAction = true,
                                sessionId = sessionId
                            )
                        }
                        com.sendra.domain.model.TransferStatus.COMPLETED -> {
                            updateNotification(
                                title = "Transfer Complete",
                                content = "All files transferred successfully",
                                progress = 100
                            )
                            delay(5000)
                            stopSelf()
                        }
                        com.sendra.domain.model.TransferStatus.FAILED -> {
                            updateNotification(
                                title = "Transfer Failed",
                                content = state.error?.message ?: "Unknown error",
                                progress = 0
                            )
                            delay(10000)
                            stopSelf()
                        }
                        else -> {}
                    }
                }
        }
    }
    
    private fun createTransferNotification(
        title: String,
        content: String,
        progress: Int,
        showResumeAction: Boolean = false,
        sessionId: String? = null
    ): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_SESSION_ID, sessionId)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val builder = NotificationCompat.Builder(this, SendraApplication.CHANNEL_TRANSFER)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_notification_transfer)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setProgress(100, progress, progress == 0)
            .setOnlyAlertOnce(true)
        
        if (showResumeAction && sessionId != null) {
            val resumeIntent = Intent(this, TransferForegroundService::class.java).apply {
                action = ACTION_RESUME
                putExtra(EXTRA_SESSION_ID, sessionId)
            }
            val resumePendingIntent = PendingIntent.getService(
                this,
                1,
                resumeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, "Resume", resumePendingIntent)
        }
        
        if (sessionId != null) {
            val cancelIntent = Intent(this, TransferForegroundService::class.java).apply {
                action = ACTION_CANCEL
                putExtra(EXTRA_SESSION_ID, sessionId)
            }
            val cancelPendingIntent = PendingIntent.getService(
                this,
                2,
                cancelIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, "Cancel", cancelPendingIntent)
        }
        
        return builder.build()
    }
    
    private fun updateNotification(
        title: String,
        content: String,
        progress: Int,
        showResumeAction: Boolean = false,
        sessionId: String? = null
    ) {
        val notification = createTransferNotification(title, content, progress, showResumeAction, sessionId)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "Sendra::TransferWakeLock"
        ).apply {
            setReferenceCounted(false)
        }
        wakeLock?.acquire(10 * 60 * 1000L) // 10 minutes max
    }
    
    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null
    }
    
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        releaseWakeLock()
        Timber.d("TransferForegroundService destroyed")
    }
    
    companion object {
        const val NOTIFICATION_ID = 1001
        const val EXTRA_SESSION_ID = "session_id"
        
        const val ACTION_START_TRANSFER = "com.sendra.START_TRANSFER"
        const val ACTION_PAUSE = "com.sendra.PAUSE"
        const val ACTION_RESUME = "com.sendra.RESUME"
        const val ACTION_CANCEL = "com.sendra.CANCEL"
        const val ACTION_STOP_SERVICE = "com.sendra.STOP_SERVICE"
    }
}
