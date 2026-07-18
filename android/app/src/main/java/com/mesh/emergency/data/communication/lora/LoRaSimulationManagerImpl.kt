/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.communication.lora

import com.mesh.emergency.core.communication.lora.LoRaSimulationConfig
import com.mesh.emergency.core.communication.lora.LoRaSimulationManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [LoRaSimulationManager] storing configuration state inside a StateFlow.
 */
@Singleton
class LoRaSimulationManagerImpl @Inject constructor() : LoRaSimulationManager {

    private val _config = MutableStateFlow(LoRaSimulationConfig())
    override val config: StateFlow<LoRaSimulationConfig> = _config.asStateFlow()

    override fun updateConfig(config: LoRaSimulationConfig) {
        _config.value = config
    }
}
