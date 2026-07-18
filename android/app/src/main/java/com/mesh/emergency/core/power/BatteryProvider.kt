/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.power

import kotlinx.coroutines.flow.Flow

/**
 * Interface contract representing hardware battery status providers.
 */
interface BatteryProvider {
    /** Streams updates of battery models properties. */
    fun getBatteryStatus(): Flow<BatteryModel>

    /** Audits active charging state. */
    fun isCharging(): Boolean

    /** Retrieves percentage level (0-100). */
    fun getBatteryLevel(): Int
}
