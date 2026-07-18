/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.di

import com.mesh.emergency.feature.emergency.data.EmergencyRepositoryImpl
import com.mesh.emergency.feature.emergency.domain.EmergencyRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class EmergencyFeatureModule {

    @Binds
    @Singleton
    abstract fun bindEmergencyRepository(impl: EmergencyRepositoryImpl): EmergencyRepository
}
