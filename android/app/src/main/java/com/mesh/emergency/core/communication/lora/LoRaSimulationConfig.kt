/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.communication.lora

/**
 * simulated LoRa transmission delay levels.
 */
enum class DelayLevel(val ms: Long) {
    LOW(50L),
    NORMAL(500L),
    HIGH(2000L)
}

/**
 * simulated LoRa signal strength attributes.
 */
enum class SimulatedSignal(val rssi: Int, val quality: String) {
    STRONG(-50, "Strong Link"),
    MEDIUM(-80, "Average Link"),
    WEAK(-110, "Marginal Link"),
    DISCONNECTED(-120, "Out of Range")
}

/**
 * Holds parameters governing Mock LoRa simulation behavior.
 */
data class LoRaSimulationConfig(
    val isSimulationEnabled: Boolean = true,
    val delayLevel: DelayLevel = DelayLevel.NORMAL,
    val packetLossRate: Float = 0.1f, // 10% packet drop rate default
    val signalStrength: SimulatedSignal = SimulatedSignal.MEDIUM,
    val simulateTimeout: Boolean = false
)
