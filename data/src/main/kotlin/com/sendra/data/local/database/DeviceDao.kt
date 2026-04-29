package com.sendra.data.local.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val type: String,
    val ipAddress: String,
    val port: Int,
    val connectionMethod: String,
    val capabilities: Int, // bitmask
    val trustStatus: String, // enum as string
    val rssi: Int?,
    val lastSeen: Long,
    val lastConnected: Long?,
    val connectionCount: Int = 0,
    val isBlocked: Boolean = false
)

@Dao
interface DeviceDao {
    @Query("SELECT * FROM devices WHERE isBlocked = 0 ORDER BY lastSeen DESC")
    fun getAllActive(): Flow<List<DeviceEntity>>
    
    @Query("SELECT * FROM devices WHERE trustStatus = 'TRUSTED' AND isBlocked = 0")
    fun getTrusted(): Flow<List<DeviceEntity>>
    
    @Query("SELECT * FROM devices WHERE id = :deviceId")
    suspend fun getById(deviceId: String): DeviceEntity?
    
    @Query("SELECT * FROM devices WHERE id = :deviceId")
    fun getByIdFlow(deviceId: String): Flow<DeviceEntity?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(device: DeviceEntity)
    
    @Update
    suspend fun update(device: DeviceEntity)
    
    @Query("UPDATE devices SET rssi = :rssi, lastSeen = :timestamp WHERE id = :deviceId")
    suspend fun updateSignal(deviceId: String, rssi: Int, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE devices SET trustStatus = :status WHERE id = :deviceId")
    suspend fun updateTrustStatus(deviceId: String, status: String)
    
    @Query("UPDATE devices SET isBlocked = :blocked WHERE id = :deviceId")
    suspend fun updateBlocked(deviceId: String, blocked: Boolean)
    
    @Query("UPDATE devices SET connectionCount = connectionCount + 1, lastConnected = :timestamp WHERE id = :deviceId")
    suspend fun recordConnection(deviceId: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("DELETE FROM devices WHERE lastSeen < :cutoff AND trustStatus != 'TRUSTED'")
    suspend fun deleteStale(cutoff: Long)
    
    @Query("DELETE FROM devices")
    suspend fun deleteAll()
}
