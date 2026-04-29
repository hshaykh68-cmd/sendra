package com.sendra.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        DeviceEntity::class,
        TransferSessionEntity::class,
        ChunkStateEntity::class,
        TransferHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SendraDatabase : RoomDatabase() {
    abstract fun deviceDao(): DeviceDao
    abstract fun transferDao(): TransferDao
    abstract fun chunkStateDao(): ChunkStateDao
    abstract fun historyDao(): HistoryDao
}
