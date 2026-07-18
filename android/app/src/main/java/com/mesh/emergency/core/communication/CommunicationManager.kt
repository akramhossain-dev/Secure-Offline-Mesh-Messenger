/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.communication

import com.mesh.emergency.core.common.result.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Coordinator regulating all local messaging pipelines and routing prioritizations.
 */
interface CommunicationManager {
    /** Emits active transceiver used for payloads transmission. */
    val activeTransport: StateFlow<Transport?>

    /** Emits overall communication status checks. */
    val communicationState: StateFlow<CommunicationState>

    /** Register a transceiver path. */
    fun registerTransport(transport: Transport)

    /** Remove a registered transceiver path. */
    fun unregisterTransport(type: TransportType)

    /** List of all registered transceivers. */
    fun getTransports(): List<Transport>

    /** List of types for transceivers currently available/ready. */
    fun getAvailableTransports(): List<TransportType>

    /**
     * Enqueues data for transmission selecting the highest connected pathway.
     */
    suspend fun sendMessage(data: ByteArray): Result<DeliveryResult>

    /** Streams merged payloads received across all active pathways. */
    fun receiveMessages(): Flow<ByteArray>
}

/**
 * States lists representing general communications layer.
 */
enum class CommunicationState {
    CONNECTED,
    DISCONNECTED,
    SEARCHING,
    SENDING,
    RECEIVING,
    FAILED,
    UNAVAILABLE
}

/**
 * States lists representing message packet transmission outcomes.
 */
enum class DeliveryResult {
    PENDING,
    SENDING,
    SENT,
    DELIVERED,
    FAILED,
    EXPIRED
}
