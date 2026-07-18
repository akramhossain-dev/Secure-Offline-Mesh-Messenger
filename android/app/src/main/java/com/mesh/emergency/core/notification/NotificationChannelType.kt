/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.notification

/**
 * Local Android notification channel types mapping priority categories.
 */
enum class NotificationChannelType {
    /** Highest priority - plays loud alerts and vibrates continuously. */
    EMERGENCY,

    /** Normal priority - incoming messages. */
    MESSAGE,

    /** Connection status changes. */
    NETWORK,

    /** Power and low battery alerts. */
    POWER,

    /** Background app execution events. */
    SYSTEM
}
