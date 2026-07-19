/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.di

import com.mesh.emergency.core.communication.CommunicationManager
import com.mesh.emergency.core.communication.Transport
import com.mesh.emergency.data.communication.CommunicationManagerImpl
import com.mesh.emergency.data.communication.bluetooth.BluetoothTransportImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module distributing transceivers and configuring the communication coordinator.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class CommunicationModule {

    /** Binds [CommunicationManagerImpl] implementation to the [CommunicationManager] interface contract. */
    @Binds
    @Singleton
    abstract fun bindCommunicationManager(impl: CommunicationManagerImpl): CommunicationManager

    /** Binds [BluetoothTransportImpl] as the primary [Transport] interface contract. */
    @Binds
    @Singleton
    abstract fun bindTransport(impl: BluetoothTransportImpl): Transport
}
