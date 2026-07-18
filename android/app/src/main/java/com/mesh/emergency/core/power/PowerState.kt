/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.power

/**
 * Battery capacity levels representation.
 */
enum class PowerState {
    NORMAL_POWER,
    LOW_BATTERY,
    CRITICAL_BATTERY,
    CHARGING,
    UNKNOWN
}
