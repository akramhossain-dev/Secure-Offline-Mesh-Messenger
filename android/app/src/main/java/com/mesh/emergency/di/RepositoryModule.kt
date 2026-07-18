/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.di

import com.mesh.emergency.data.repository.CommunicationRepositoryImpl
import com.mesh.emergency.data.repository.DeviceRepositoryImpl
import com.mesh.emergency.data.repository.MessageRepositoryImpl
import com.mesh.emergency.data.repository.SettingsRepositoryImpl
import com.mesh.emergency.data.repository.UserRepositoryImpl
import com.mesh.emergency.domain.repository.CommunicationRepository
import com.mesh.emergency.domain.repository.DeviceRepository
import com.mesh.emergency.domain.repository.MessageRepository
import com.mesh.emergency.domain.repository.SettingsRepository
import com.mesh.emergency.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module binding domain repositories contracts to their respective data-layer implementations.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /** Binds [UserRepositoryImpl] to the [UserRepository] interface. */
    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    /** Binds [MessageRepositoryImpl] to the [MessageRepository] interface. */
    @Binds
    @Singleton
    abstract fun bindMessageRepository(impl: MessageRepositoryImpl): MessageRepository

    /** Binds [DeviceRepositoryImpl] to the [DeviceRepository] interface. */
    @Binds
    @Singleton
    abstract fun bindDeviceRepository(impl: DeviceRepositoryImpl): DeviceRepository

    /** Binds [SettingsRepositoryImpl] to the [SettingsRepository] interface. */
    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    /** Binds [CommunicationRepositoryImpl] to the [CommunicationRepository] interface. */
    @Binds
    @Singleton
    abstract fun bindCommunicationRepository(impl: CommunicationRepositoryImpl): CommunicationRepository
}
