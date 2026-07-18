/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.communication

import com.mesh.emergency.core.common.result.Result
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface contract for peripheral and mesh hardware communications management.
 */
interface TransportManager {
    /**
     * Emits true if a communication pathway (BLE or LoRa) is presently active.
     */
    val isTransportAvailable: StateFlow<Boolean>

    /**
     * Emits connection status of the peripheral mesh hardware bridge.
     */
    val connectionState: StateFlow<ConnectionState>

    /**
     * Enqueues data for transmission across the active channel.
     */
    suspend fun sendData(data: ByteArray): Result<Unit>
}

/**
 * Lifecycle connection states for physical mesh hardware link.
 */
enum class ConnectionState {
    CONNECTED,
    DISCONNECTED,
    CONNECTING,
    RECONNECTING
}
