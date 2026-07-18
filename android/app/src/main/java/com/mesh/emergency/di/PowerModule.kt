/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.di

import com.mesh.emergency.core.power.BatteryProvider
import com.mesh.emergency.core.power.PowerManager
import com.mesh.emergency.data.power.BatteryProviderStub
import com.mesh.emergency.data.power.PowerManagerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module binding battery status providers and power saving managers.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PowerModule {

    /** Binds [BatteryProviderStub] to the [BatteryProvider] interface. */
    @Binds
    @Singleton
    abstract fun bindBatteryProvider(impl: BatteryProviderStub): BatteryProvider

    /** Binds [PowerManagerImpl] to the [PowerManager] interface. */
    @Binds
    @Singleton
    abstract fun bindPowerManager(impl: PowerManagerImpl): PowerManager
}
