/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.di

import com.mesh.emergency.core.resource.ResourceManager
import com.mesh.emergency.data.repository.ResourceRepositoryImpl
import com.mesh.emergency.data.resource.ResourceManagerImpl
import com.mesh.emergency.domain.repository.ResourceRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module binding local resource repository and manager interfaces.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ResourceModule {

    /** Binds [ResourceRepositoryImpl] to the [ResourceRepository] interface. */
    @Binds
    @Singleton
    abstract fun bindResourceRepository(impl: ResourceRepositoryImpl): ResourceRepository

    /** Binds [ResourceManagerImpl] to the [ResourceManager] interface. */
    @Binds
    @Singleton
    abstract fun bindResourceManager(impl: ResourceManagerImpl): ResourceManager
}
