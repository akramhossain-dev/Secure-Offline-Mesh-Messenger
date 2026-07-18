/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.system

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [NotificationServiceWrapper] wrapping [NotificationManagerCompat].
 */
@Singleton
class NotificationServiceWrapperImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : NotificationServiceWrapper {

    private val notificationManager = NotificationManagerCompat.from(context)

    override fun areNotificationsEnabled(): Boolean {
        return notificationManager.areNotificationsEnabled()
    }

    override fun cancelAllNotifications() {
        notificationManager.cancelAll()
    }
}
