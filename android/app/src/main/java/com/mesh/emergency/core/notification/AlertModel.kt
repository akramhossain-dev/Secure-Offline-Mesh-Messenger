/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.notification

/**
 * Data Model mapping notification alert properties.
 */
data class AlertModel(
    val id: String,
    val type: AlertType,
    val title: String,
    val description: String,
    val priority: AlertPriority,
    val timestamp: Long,
    val source: String,
    val status: String
)

/**
 * Alert classification types.
 */
enum class AlertType {
    SOS_ALERT,
    MESSAGE_ALERT,
    NODE_ALERT,
    BATTERY_ALERT,
    SYSTEM_ALERT
}

/**
 * Alert system priorities.
 */
enum class AlertPriority {
    CRITICAL,
    HIGH,
    NORMAL,
    LOW
}
