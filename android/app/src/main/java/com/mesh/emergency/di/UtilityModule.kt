/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.di

import com.mesh.emergency.core.hardware.bluetooth.BluetoothManager
import com.mesh.emergency.core.map.MapProvider
import com.mesh.emergency.core.system.AudioServiceWrapper
import com.mesh.emergency.core.system.AudioServiceWrapperImpl
import com.mesh.emergency.core.system.BluetoothServiceWrapper
import com.mesh.emergency.core.system.BluetoothServiceWrapperImpl
import com.mesh.emergency.core.system.LocationServiceWrapper
import com.mesh.emergency.core.system.LocationServiceWrapperImpl
import com.mesh.emergency.core.system.NotificationServiceWrapper
import com.mesh.emergency.core.system.NotificationServiceWrapperImpl
import com.mesh.emergency.core.utils.LocationProvider
import com.mesh.emergency.core.utils.LocationProviderStub
import com.mesh.emergency.core.utils.PermissionManager
import com.mesh.emergency.core.utils.PermissionManagerImpl
import com.mesh.emergency.core.utils.capability.DeviceCapabilityManager
import com.mesh.emergency.core.utils.capability.DeviceCapabilityManagerImpl
import com.mesh.emergency.data.hardware.bluetooth.BluetoothManagerImpl
import com.mesh.emergency.data.map.MapProviderImpl
import com.mesh.emergency.data.repository.LocationRepositoryImpl
import com.mesh.emergency.domain.repository.LocationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module binding utility manager, location repository, and offline map provider interfaces.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class UtilityModule {

    /** Binds [PermissionManagerImpl] to the [PermissionManager] interface. */
    @Binds
    @Singleton
    abstract fun bindPermissionManager(impl: PermissionManagerImpl): PermissionManager

    /** Binds [LocationProviderStub] to the [LocationProvider] interface. */
    @Binds
    @Singleton
    abstract fun bindLocationProvider(impl: LocationProviderStub): LocationProvider

    /** Binds [DeviceCapabilityManagerImpl] to the [DeviceCapabilityManager] interface. */
    @Binds
    @Singleton
    abstract fun bindDeviceCapabilityManager(impl: DeviceCapabilityManagerImpl): DeviceCapabilityManager

    /** Binds [BluetoothServiceWrapperImpl] to the [BluetoothServiceWrapper] interface. */
    @Binds
    @Singleton
    abstract fun bindBluetoothServiceWrapper(impl: BluetoothServiceWrapperImpl): BluetoothServiceWrapper

    /** Binds [LocationServiceWrapperImpl] to the [LocationServiceWrapper] interface. */
    @Binds
    @Singleton
    abstract fun bindLocationServiceWrapper(impl: LocationServiceWrapperImpl): LocationServiceWrapper

    /** Binds [NotificationServiceWrapperImpl] to the [NotificationServiceWrapper] interface. */
    @Binds
    @Singleton
    abstract fun bindNotificationServiceWrapper(impl: NotificationServiceWrapperImpl): NotificationServiceWrapper

    /** Binds [AudioServiceWrapperImpl] to the [AudioServiceWrapper] interface. */
    @Binds
    @Singleton
    abstract fun bindAudioServiceWrapper(impl: AudioServiceWrapperImpl): AudioServiceWrapper

    /** Binds [BluetoothManagerImpl] to the [BluetoothManager] interface. */
    @Binds
    @Singleton
    abstract fun bindBluetoothManager(impl: BluetoothManagerImpl): BluetoothManager

    /** Binds [LocationRepositoryImpl] to the [LocationRepository] interface. */
    @Binds
    @Singleton
    abstract fun bindLocationRepository(impl: LocationRepositoryImpl): LocationRepository

    /** Binds [MapProviderImpl] to the [MapProvider] interface. */
    @Binds
    @Singleton
    abstract fun bindMapProvider(impl: MapProviderImpl): MapProvider
}
