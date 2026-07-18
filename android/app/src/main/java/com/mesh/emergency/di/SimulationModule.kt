/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.di

import com.mesh.emergency.core.communication.lora.LoRaSimulationManager
import com.mesh.emergency.data.communication.lora.LoRaSimulationManagerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module binding LoRa simulation management interfaces.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SimulationModule {

    /** Binds [LoRaSimulationManagerImpl] to the [LoRaSimulationManager] interface. */
    @Binds
    @Singleton
    abstract fun bindLoRaSimulationManager(impl: LoRaSimulationManagerImpl): LoRaSimulationManager
}
