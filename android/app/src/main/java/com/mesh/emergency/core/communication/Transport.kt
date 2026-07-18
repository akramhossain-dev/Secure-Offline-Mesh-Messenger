/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.communication

import com.mesh.emergency.core.common.result.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Represents a physical or simulated network transceiver connection pathway.
 */
interface Transport {
    /** Transceiver type categorizer identifier. */
    val type: TransportType

    /** Current connection and capability status flow. */
    val status: StateFlow<TransportStatus>

    /** Commands transport to bridge connection. */
    suspend fun connect(): Result<Unit>

    /** Commands transport to drop connection. */
    suspend fun disconnect(): Result<Unit>

    /** Commands transport to enqueue/send raw bytes payload. */
    suspend fun send(data: ByteArray): Result<Unit>

    /** Streams raw bytes payloads received by this transceiver. */
    fun receive(): Flow<ByteArray>
}

/**
 * Types lists matching network layers.
 */
enum class TransportType {
    BLUETOOTH,
    LORA,
    MOCK
}

/**
 * States lists representing hardware connectivity.
 */
enum class TransportStatus {
    CONNECTED,
    DISCONNECTED,
    CONNECTING,
    UNAVAILABLE
}
