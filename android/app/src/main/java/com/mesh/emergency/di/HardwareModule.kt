/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.di

import com.mesh.emergency.core.hardware.BleDeviceRepository
import com.mesh.emergency.core.hardware.HardwareManager
import com.mesh.emergency.hardware.HardwareManagerImpl
import com.mesh.emergency.hardware.bluetooth.BleDeviceRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt bindings for the A30 + A31 hardware communication layer.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class HardwareModule {

    @Binds
    @Singleton
    abstract fun bindBleDeviceRepository(impl: BleDeviceRepositoryImpl): BleDeviceRepository

    @Binds
    @Singleton
    abstract fun bindHardwareManager(impl: HardwareManagerImpl): HardwareManager
}
