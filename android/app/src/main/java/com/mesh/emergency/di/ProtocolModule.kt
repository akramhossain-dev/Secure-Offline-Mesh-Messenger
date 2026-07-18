/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.di

import com.mesh.emergency.core.protocol.DuplicateDetector
import com.mesh.emergency.core.protocol.PacketValidator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module distributing protocol validation and deduplication singletons.
 */
@Module
@InstallIn(SingletonComponent::class)
object ProtocolModule {

    @Provides
    @Singleton
    fun provideDuplicateDetector(): DuplicateDetector = DuplicateDetector()

    @Provides
    @Singleton
    fun providePacketValidator(): PacketValidator = PacketValidator()
}
