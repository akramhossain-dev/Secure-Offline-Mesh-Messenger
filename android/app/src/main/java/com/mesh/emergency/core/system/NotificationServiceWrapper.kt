/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.system

/**
 * Interface contract wrapping Android NotificationManagerCompat calls.
 */
interface NotificationServiceWrapper {
    /** Returns true if push alerts are permitted by system filters. */
    fun areNotificationsEnabled(): Boolean

    /** Clears all published notification bubbles. */
    fun cancelAllNotifications()
}
