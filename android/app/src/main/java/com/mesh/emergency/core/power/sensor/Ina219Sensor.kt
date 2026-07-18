/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.power.sensor

/**
 * Interface contract for external current sensors (e.g. INA219 current/power monitors).
 */
interface Ina219Sensor {
    /** Voltage metric reading (Volts). */
    fun getVoltage(): Double

    /** Current metric reading (Amperes). */
    fun getCurrent(): Double

    /** Power consumption rating calculation (Watts). */
    fun getPowerConsumption(): Double

    /** Computed battery level mapping percentage. */
    fun getBatteryPercentage(): Int
}
