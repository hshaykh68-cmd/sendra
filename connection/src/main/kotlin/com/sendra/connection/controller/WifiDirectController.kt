package com.sendra.connection.controller

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pGroup
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Looper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WifiDirectController @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val wifiP2pManager: WifiP2pManager? = context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    private val wifiP2pChannel: WifiP2pManager.Channel? = wifiP2pManager?.initialize(
        context,
        Looper.getMainLooper(),
        null
    )
    
    private var isGroupOwner = false
    private var groupInfo: WifiP2pGroup? = null
    
    suspend fun createGroup(): Result<Unit> {
        if (wifiP2pManager == null || wifiP2pChannel == null) {
            return Result.failure(IllegalStateException("WiFi Direct not available"))
        }

        val deferred = CompletableDeferred<Result<Unit>>()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                wifiP2pManager.createGroup(wifiP2pChannel, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        isGroupOwner = true
                        deferred.complete(Result.success(Unit))
                    }
                    
                    override fun onFailure(reason: Int) {
                        deferred.complete(Result.failure(Exception("Failed to create group: $reason")))
                    }
                })
            } else {
                @Suppress("DEPRECATION")
                wifiP2pManager.createGroup(wifiP2pChannel, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        isGroupOwner = true
                        deferred.complete(Result.success(Unit))
                    }
                    
                    override fun onFailure(reason: Int) {
                        deferred.complete(Result.failure(Exception("Failed to create group: $reason")))
                    }
                })
            }
        } catch (e: Exception) {
            deferred.complete(Result.failure(e))
        }
        
        return deferred.await()
    }
    
    suspend fun connectToDevice(deviceAddress: String): Result<Unit> {
        if (wifiP2pManager == null || wifiP2pChannel == null) {
            return Result.failure(IllegalStateException("WiFi Direct not available"))
        }

        val deferred = CompletableDeferred<Result<Unit>>()

        val config = WifiP2pConfig.Builder()
            .setDeviceAddress(android.net.MacAddress.fromString(deviceAddress))
            .build()

        wifiP2pManager.connect(wifiP2pChannel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                deferred.complete(Result.success(Unit))
            }
            
            override fun onFailure(reason: Int) {
                deferred.complete(Result.failure(Exception("Connection failed: $reason")))
            }
        })
        
        return deferred.await()
    }
    
    fun disconnect(): Result<Unit> {
        if (wifiP2pManager == null || wifiP2pChannel == null) {
            return Result.failure(IllegalStateException("WiFi Direct not available"))
        }

        try {
            wifiP2pManager.removeGroup(wifiP2pChannel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    isGroupOwner = false
                    groupInfo = null
                    Timber.d("WiFi Direct group removed")
                }
                
                override fun onFailure(reason: Int) {
                    Timber.w("Failed to remove WiFi Direct group: $reason")
                }
            })
            return Result.success(Unit)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
    
    fun getConnectionInfo(): Flow<WifiDirectConnectionInfo> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                        val networkInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO, android.net.NetworkInfo::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO)
                        }
                        
                        wifiP2pManager?.requestConnectionInfo(wifiP2pChannel) { info ->
                            isGroupOwner = info.isGroupOwner
                            
                            if (info.groupFormed) {
                                trySend(WifiDirectConnectionInfo.Connected(
                                    isGroupOwner = info.isGroupOwner,
                                    groupOwnerAddress = info.groupOwnerAddress?.hostAddress
                                ))
                            } else {
                                trySend(WifiDirectConnectionInfo.Disconnected)
                            }
                        }
                    }
                    
                    WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                        val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE, WifiP2pDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE)
                        }
                        
                        device?.let {
                            trySend(WifiDirectConnectionInfo.DeviceChanged(
                                deviceName = it.deviceName,
                                deviceAddress = it.deviceAddress,
                                status = it.status
                            ))
                        }
                    }
                }
            }
        }
        
        val intentFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }
        
        context.registerReceiver(receiver, intentFilter)
        
        awaitClose {
            context.unregisterReceiver(receiver)
        }
    }
    
    fun getGroupOwnerAddress(): String? {
        return if (isGroupOwner) {
            // We are the group owner, return our address
            getLocalIpAddress()
        } else {
            // We are a client, need to discover group owner
            null
        }
    }
    
    private fun getLocalIpAddress(): String? {
        // Get the IP address from the WiFi P2P interface
        // This requires more complex network interface enumeration
        return null
    }
}

sealed class WifiDirectConnectionInfo {
    data class Connected(
        val isGroupOwner: Boolean,
        val groupOwnerAddress: String?
    ) : WifiDirectConnectionInfo()
    
    data class DeviceChanged(
        val deviceName: String,
        val deviceAddress: String,
        val status: Int
    ) : WifiDirectConnectionInfo()
    
    object Disconnected : WifiDirectConnectionInfo()
}
