package com.sendra.app.di

import android.content.Context
import androidx.room.Room
import com.sendra.data.local.database.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SendraDatabase {
        return Room.databaseBuilder(
            context,
            SendraDatabase::class.java,
            "sendra_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }
    
    @Provides
    fun provideDeviceDao(database: SendraDatabase): DeviceDao {
        return database.deviceDao()
    }
    
    @Provides
    fun provideTransferDao(database: SendraDatabase): TransferDao {
        return database.transferDao()
    }
    
    @Provides
    fun provideChunkStateDao(database: SendraDatabase): ChunkStateDao {
        return database.chunkStateDao()
    }
    
    @Provides
    fun provideHistoryDao(database: SendraDatabase): HistoryDao {
        return database.historyDao()
    }
}
