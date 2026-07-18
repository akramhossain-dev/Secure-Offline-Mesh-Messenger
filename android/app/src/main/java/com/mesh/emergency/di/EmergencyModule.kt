/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.di

import com.mesh.emergency.core.emergency.EmergencyManager
import com.mesh.emergency.data.emergency.EmergencyManagerImpl
import com.mesh.emergency.data.repository.EmergencyRepositoryImpl
import com.mesh.emergency.domain.repository.EmergencyRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module binding local Emergency manager and repository interfaces.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class EmergencyModule {

    /** Binds [EmergencyRepositoryImpl] to the [EmergencyRepository] interface. */
    @Binds
    @Singleton
    abstract fun bindEmergencyRepository(impl: EmergencyRepositoryImpl): EmergencyRepository

    /** Binds [EmergencyManagerImpl] to the [EmergencyManager] interface. */
    @Binds
    @Singleton
    abstract fun bindEmergencyManager(impl: EmergencyManagerImpl): EmergencyManager
}
