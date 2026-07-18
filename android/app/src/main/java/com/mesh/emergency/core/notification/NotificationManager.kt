/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.notification

import com.mesh.emergency.core.common.result.Result

/**
 * Interface contract coordinating notification alerts dispatching and channel preferences.
 */
interface NotificationManager {
    /** Dispatches an alert notification bubble. */
    fun showNotification(alert: AlertModel): Result<Unit>

    /** Dismisses an active notification by ID. */
    fun cancelNotification(alertId: String): Result<Unit>

    /** Initializes high/low priority channels in Android System. */
    fun createNotificationChannels(): Result<Unit>

    /** Modifies channel preference parameters. */
    fun updatePreferences(
        channel: NotificationChannelType,
        enabled: Boolean,
        sound: Boolean,
        vibration: Boolean
    ): Result<Unit>
}
