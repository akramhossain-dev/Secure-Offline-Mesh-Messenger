/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.notification

import com.mesh.emergency.core.common.result.Result
import com.mesh.emergency.core.notification.AlertModel
import com.mesh.emergency.core.notification.AlertPriority
import com.mesh.emergency.core.notification.NotificationChannelType
import com.mesh.emergency.core.notification.NotificationManager
import com.mesh.emergency.core.system.NotificationServiceWrapper
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [NotificationManager] coordinating alerts rendering.
 */
@Singleton
class NotificationManagerImpl @Inject constructor(
    private val notificationServiceWrapper: NotificationServiceWrapper
) : NotificationManager {

    private val preferences = mutableMapOf<NotificationChannelType, ChannelPref>()

    override fun showNotification(alert: AlertModel): Result<Unit> {
        if (!notificationServiceWrapper.areNotificationsEnabled()) {
            return Result.Error(Exception("Notifications disabled in OS settings"))
        }

        val channel = when (alert.priority) {
            AlertPriority.CRITICAL -> NotificationChannelType.EMERGENCY
            AlertPriority.HIGH     -> NotificationChannelType.POWER
            AlertPriority.NORMAL   -> NotificationChannelType.MESSAGE
            AlertPriority.LOW      -> NotificationChannelType.SYSTEM
        }

        val pref = preferences[channel] ?: ChannelPref(true, true, true)
        if (!pref.enabled) {
            return Result.Success(Unit) // Silenced
        }

        // Simulates system dispatch
        println("[NOTIFY] [${channel.name}] Title:${alert.title} Desc:${alert.description} Priority:${alert.priority.name}")
        return Result.Success(Unit)
    }

    override fun cancelNotification(alertId: String): Result<Unit> {
        return Result.Success(Unit)
    }

    override fun createNotificationChannels(): Result<Unit> {
        NotificationChannelType.values().forEach {
            preferences[it] = ChannelPref(true, true, true)
        }
        return Result.Success(Unit)
    }

    override fun updatePreferences(
        channel: NotificationChannelType,
        enabled: Boolean,
        sound: Boolean,
        vibration: Boolean
    ): Result<Unit> {
        preferences[channel] = ChannelPref(enabled, sound, vibration)
        return Result.Success(Unit)
    }

    private data class ChannelPref(
        val enabled: Boolean,
        val sound: Boolean,
        val vibration: Boolean
    )
}
