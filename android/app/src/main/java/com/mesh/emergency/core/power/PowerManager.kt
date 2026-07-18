/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.power

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface contract coordinating power management strategies.
 */
interface PowerManager {
    /** Exposes the active power profile mode. */
    val currentMode: StateFlow<PowerSavingMode>

    /** Exposes power event warnings. */
    val powerEvents: SharedFlow<PowerEvent>

    /** Configures a custom power profile. */
    fun setPowerMode(mode: PowerSavingMode)

    /** Evaluates battery updates to trigger profile transitions. */
    fun handleBatteryUpdate(level: Int, isCharging: Boolean)
}

/**
 * Battery alerts events.
 */
sealed class PowerEvent {
    object BatteryLow : PowerEvent()
    object ChargingStarted : PowerEvent()
    object ChargingStopped : PowerEvent()
    object PowerCritical : PowerEvent()
}
