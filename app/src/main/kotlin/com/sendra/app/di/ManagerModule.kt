package com.sendra.app.di

import com.sendra.connection.manager.ConnectionManager
import com.sendra.connection.manager.ConnectionManagerImpl
import com.sendra.data.repository.DeviceRepositoryImpl
import com.sendra.data.repository.FileRepositoryImpl
import com.sendra.data.repository.TransferRepositoryImpl
import com.sendra.discovery.manager.DiscoveryManagerImpl
import com.sendra.domain.repository.DeviceRepository
import com.sendra.domain.repository.FileRepository
import com.sendra.domain.repository.TransferRepository
import com.sendra.domain.usecase.discovery.DiscoveryManager
import com.sendra.transfer.manager.TransferManager
import com.sendra.transfer.manager.TransferManagerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ManagerModule {
    
    @Binds
    @Singleton
    abstract fun bindDiscoveryManager(
        impl: DiscoveryManagerImpl
    ): DiscoveryManager
    
    @Binds
    @Singleton
    abstract fun bindConnectionManager(
        impl: ConnectionManagerImpl
    ): ConnectionManager
    
    @Binds
    @Singleton
    abstract fun bindTransferManager(
        impl: TransferManagerImpl
    ): TransferManager
    
    @Binds
    @Singleton
    abstract fun bindDeviceRepository(
        impl: DeviceRepositoryImpl
    ): DeviceRepository
    
    @Binds
    @Singleton
    abstract fun bindFileRepository(
        impl: FileRepositoryImpl
    ): FileRepository
    
    @Binds
    @Singleton
    abstract fun bindTransferRepository(
        impl: TransferRepositoryImpl
    ): TransferRepository
}
