/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.di

import com.mesh.emergency.core.communication.CommunicationManager
import com.mesh.emergency.core.communication.Transport
import com.mesh.emergency.data.communication.BluetoothTransportStub
import com.mesh.emergency.data.communication.CommunicationManagerImpl
import com.mesh.emergency.data.communication.LoRaTransportStub
import com.mesh.emergency.data.communication.MockTransport
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BluetoothTransportQualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class LoRaTransportQualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MockTransportQualifier

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

    companion object {

        @Provides
        @Singleton
        @BluetoothTransportQualifier
        fun provideBluetoothTransport(stub: BluetoothTransportStub): Transport = stub

        @Provides
        @Singleton
        @LoRaTransportQualifier
        fun provideLoRaTransport(stub: LoRaTransportStub): Transport = stub

        @Provides
        @Singleton
        @MockTransportQualifier
        fun provideMockTransport(mock: MockTransport): Transport = mock
    }
}
