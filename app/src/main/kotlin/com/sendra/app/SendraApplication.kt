package com.sendra.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import android.util.Log
import javax.inject.Inject

@HiltAndroidApp
class SendraApplication : Application(), Configuration.Provider {
    
    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize logging
        if (BuildConfig.DEBUG) {
            Log.d("SendraApplication", "Debug mode enabled")
        }
        
        // Create notification channel for foreground service
        createNotificationChannels()
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val transferChannel = NotificationChannel(
                CHANNEL_TRANSFER,
                "File Transfers",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress of active file transfers"
                setSound(null, null)
                enableVibration(false)
            }
            
            val discoveryChannel = NotificationChannel(
                CHANNEL_DISCOVERY,
                "Incoming Transfers",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when someone wants to send you files"
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannels(listOf(transferChannel, discoveryChannel))
        }
    }
    
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
    
    companion object {
        const val CHANNEL_TRANSFER = "sendra_transfer"
        const val CHANNEL_DISCOVERY = "sendra_discovery"
    }
}
