package com.sendra.data.local.database

import androidx.room.*

@Dao
interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(entry: TransferHistoryEntity)

    @Query("SELECT * FROM transfer_history ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentHistory(limit: Int = 100): List<TransferHistoryEntity>

    @Query("DELETE FROM transfer_history WHERE timestamp < :cutoff")
    suspend fun deleteOldHistory(cutoff: Long)
}
