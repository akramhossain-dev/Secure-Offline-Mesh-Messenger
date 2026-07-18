/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.di

import com.mesh.emergency.core.common.config.AppConfiguration
import com.mesh.emergency.core.common.config.AppConfigurationImpl
import com.mesh.emergency.core.common.logging.Logger
import com.mesh.emergency.core.common.logging.LoggerImpl
import com.mesh.emergency.core.communication.TransportManager
import com.mesh.emergency.core.communication.TransportManagerStub
import com.mesh.emergency.core.storage.StorageManager
import com.mesh.emergency.core.storage.StorageManagerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module binding core application infrastructure interface dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class CoreModule {

    /** Binds [LoggerImpl] implementation to the [Logger] interface contract. */
    @Binds
    @Singleton
    abstract fun bindLogger(impl: LoggerImpl): Logger

    /** Binds [StorageManagerImpl] implementation to the [StorageManager] interface contract. */
    @Binds
    @Singleton
    abstract fun bindStorageManager(impl: StorageManagerImpl): StorageManager

    /** Binds [TransportManagerStub] implementation to the [TransportManager] interface contract. */
    @Binds
    @Singleton
    abstract fun bindTransportManager(impl: TransportManagerStub): TransportManager

    /** Binds [AppConfigurationImpl] implementation to the [AppConfiguration] interface contract. */
    @Binds
    @Singleton
    abstract fun bindAppConfiguration(impl: AppConfigurationImpl): AppConfiguration
}
