/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.di

import com.mesh.emergency.core.map.MapRepository
import com.mesh.emergency.data.map.MapProviderImpl
import com.mesh.emergency.data.map.MapRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing offline map dependencies.
 *
 * Binds [MapRepositoryImpl] to [MapRepository] interface.
 * [MapProviderImpl] is bound to [com.mesh.emergency.core.map.MapProvider] in the existing MapModule
 * or directly injected as a concrete type for layer management.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class MapModule {

    /** Binds [MapRepositoryImpl] to [MapRepository] contract. */
    @Binds
    @Singleton
    abstract fun bindMapRepository(impl: MapRepositoryImpl): MapRepository
}
