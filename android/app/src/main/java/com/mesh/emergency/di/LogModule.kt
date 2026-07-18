/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.di

import com.mesh.emergency.core.log.LoggerManager
import com.mesh.emergency.data.log.LoggerManagerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module binding local logging and diagnostics managers.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class LogModule {

    /** Binds [LoggerManagerImpl] to the [LoggerManager] interface. */
    @Binds
    @Singleton
    abstract fun bindLoggerManager(impl: LoggerManagerImpl): LoggerManager
}
