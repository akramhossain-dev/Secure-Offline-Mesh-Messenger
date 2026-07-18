/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.power

/**
 * Data Model mapping current battery telemetry parameters.
 */
data class BatteryModel(
    val level: Int,
    val isCharging: Boolean,
    val powerSource: String,
    val health: String,
    val temperature: Float,
    val timestamp: Long
)
