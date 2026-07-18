/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.di

import com.mesh.emergency.core.discovery.DiscoveryManager
import com.mesh.emergency.core.discovery.qr.QRHandshakeManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module distributing device pairing and discovery singletons.
 */
@Module
@InstallIn(SingletonComponent::class)
object DiscoveryModule {

    @Provides
    @Singleton
    fun provideDiscoveryManager(): DiscoveryManager = DiscoveryManager()

    @Provides
    @Singleton
    fun provideQRHandshakeManager(): QRHandshakeManager = QRHandshakeManager
}
