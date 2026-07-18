/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.di

import androidx.hilt.work.HiltWorkerFactory
import androidx.work.WorkerFactory
import com.mesh.emergency.core.location.LocationSharingManager
import com.mesh.emergency.data.location.LocationSharingManagerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module binding WorkManager workers and location sharing manager.
 *
 * Workers use @HiltWorker + @AssistedInject which are auto-registered via
 * HiltWorkerFactory — no explicit binding per worker is needed.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class WorkerModule {

    /** Binds [LocationSharingManagerImpl] to [LocationSharingManager] interface. */
    @Binds
    @Singleton
    abstract fun bindLocationSharingManager(impl: LocationSharingManagerImpl): LocationSharingManager
}
