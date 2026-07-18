/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.communication.lora

import kotlinx.coroutines.flow.StateFlow

/**
 * Controller allowing development tools or tests to adjust LoRa simulation attributes.
 */
interface LoRaSimulationManager {
    /** Exposes active configuration settings state. */
    val config: StateFlow<LoRaSimulationConfig>

    /** Modifies the active configuration settings. */
    fun updateConfig(config: LoRaSimulationConfig)
}
