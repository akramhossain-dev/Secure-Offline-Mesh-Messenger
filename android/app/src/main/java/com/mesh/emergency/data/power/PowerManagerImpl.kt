/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.power

import com.mesh.emergency.core.power.PowerEvent
import com.mesh.emergency.core.power.PowerManager
import com.mesh.emergency.core.power.PowerSavingMode
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [PowerManager] evaluating level thresholds to adjust performance profiles.
 */
@Singleton
class PowerManagerImpl @Inject constructor() : PowerManager {

    private val _currentMode = MutableStateFlow(PowerSavingMode.NORMAL)
    override val currentMode: StateFlow<PowerSavingMode> = _currentMode.asStateFlow()

    private val _powerEvents = MutableSharedFlow<PowerEvent>(extraBufferCapacity = 64)
    override val powerEvents: SharedFlow<PowerEvent> = _powerEvents.asSharedFlow()

    private var wasCharging = false

    override fun setPowerMode(mode: PowerSavingMode) {
        _currentMode.value = mode
    }

    override fun handleBatteryUpdate(level: Int, isCharging: Boolean) {
        // Charging events audits
        if (isCharging && !wasCharging) {
            _powerEvents.tryEmit(PowerEvent.ChargingStarted)
        } else if (!isCharging && wasCharging) {
            _powerEvents.tryEmit(PowerEvent.ChargingStopped)
        }
        wasCharging = isCharging

        if (isCharging) {
            _currentMode.value = PowerSavingMode.NORMAL
            return
        }

        // Evaluate levels
        when {
            level <= 10 -> {
                _currentMode.value = PowerSavingMode.EMERGENCY
                _powerEvents.tryEmit(PowerEvent.PowerCritical)
            }
            level <= 20 -> {
                _currentMode.value = PowerSavingMode.SAVING
                _powerEvents.tryEmit(PowerEvent.BatteryLow)
            }
            else -> {
                _currentMode.value = PowerSavingMode.NORMAL
            }
        }
    }
}
