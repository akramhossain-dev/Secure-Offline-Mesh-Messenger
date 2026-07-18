/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.di

import com.mesh.emergency.core.utils.LocationProvider
import com.mesh.emergency.core.utils.LocationProviderStub
import com.mesh.emergency.core.utils.PermissionManager
import com.mesh.emergency.core.utils.PermissionManagerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module binding utility manager interfaces.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class UtilityModule {

    /** Binds [PermissionManagerImpl] to the [PermissionManager] interface. */
    @Binds
    @Singleton
    abstract fun bindPermissionManager(impl: PermissionManagerImpl): PermissionManager

    /** Binds [LocationProviderStub] to the [LocationProvider] interface. */
    @Binds
    @Singleton
    abstract fun bindLocationProvider(impl: LocationProviderStub): LocationProvider
}
