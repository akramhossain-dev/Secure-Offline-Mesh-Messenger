/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.di

import com.mesh.emergency.core.notification.NotificationManager
import com.mesh.emergency.data.notification.NotificationManagerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module binding local alert dispatchers and notification managers.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationModule {

    /** Binds [NotificationManagerImpl] to the [NotificationManager] interface. */
    @Binds
    @Singleton
    abstract fun bindNotificationManager(impl: NotificationManagerImpl): NotificationManager
}
