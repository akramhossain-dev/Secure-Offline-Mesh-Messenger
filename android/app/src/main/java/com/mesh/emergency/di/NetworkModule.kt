/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.di

import com.mesh.emergency.core.network.NetworkHealthManager
import com.mesh.emergency.core.network.NodeDiscoveryManager
import com.mesh.emergency.data.network.NetworkHealthManagerImpl
import com.mesh.emergency.data.network.NodeDiscoveryManagerImpl
import com.mesh.emergency.data.repository.NodeRepositoryImpl
import com.mesh.emergency.domain.repository.NodeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module binding local network awareness coordinators and repositories.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkModule {

    /** Binds [NodeRepositoryImpl] to the [NodeRepository] interface. */
    @Binds
    @Singleton
    abstract fun bindNodeRepository(impl: NodeRepositoryImpl): NodeRepository

    /** Binds [NodeDiscoveryManagerImpl] to the [NodeDiscoveryManager] interface. */
    @Binds
    @Singleton
    abstract fun bindNodeDiscoveryManager(impl: NodeDiscoveryManagerImpl): NodeDiscoveryManager

    /** Binds [NetworkHealthManagerImpl] to the [NetworkHealthManager] interface. */
    @Binds
    @Singleton
    abstract fun bindNetworkHealthManager(impl: NetworkHealthManagerImpl): NetworkHealthManager
}
