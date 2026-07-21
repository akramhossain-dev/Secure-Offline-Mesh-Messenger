/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.communication.nfc

import com.mesh.emergency.core.common.result.Result
import com.mesh.emergency.core.communication.Transport
import com.mesh.emergency.core.communication.TransportCapability
import com.mesh.emergency.core.communication.TransportEvent
import com.mesh.emergency.core.communication.TransportNode
import com.mesh.emergency.core.communication.TransportStatus
import com.mesh.emergency.core.communication.TransportType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Extension point for a future NFC transport implementation.
 *
 * NFC enables tap-to-connect pairing and short-burst data transfer at very close range.
 * Most useful for initial device pairing (replacing QR codes) and small data payloads.
 *
 * This stub satisfies the [Transport] interface contract and reports [TransportStatus.UNAVAILABLE].
 * When the NFC transport is implemented, only this class needs to change —
 * no modifications to [CommunicationManager], [MessagingService], or any UI layer.
 *
 * Expected capabilities when implemented:
 * - [TransportCapability.ACKNOWLEDGEMENTS] — NDEF write/read confirms delivery
 */
@Singleton
class NfcTransportStub @Inject constructor() : Transport {

    override val type: TransportType = TransportType.NFC

    override val capabilities: Set<TransportCapability> = setOf(
        TransportCapability.ACKNOWLEDGEMENTS
    )

    private val _status = MutableStateFlow(TransportStatus.UNAVAILABLE)
    override val status: StateFlow<TransportStatus> = _status.asStateFlow()

    override suspend fun start(): Result<Unit>           = Result.Success(Unit) // no-op stub
    override suspend fun stop(): Result<Unit>            = Result.Success(Unit) // no-op stub
    override suspend fun connect(): Result<Unit>         = Result.Error(Exception("NFC transport not yet implemented"))
    override suspend fun disconnect(): Result<Unit>      = Result.Success(Unit) // no-op stub
    override suspend fun advertise(): Result<Unit>       = Result.Error(Exception("NFC transport not yet implemented"))
    override suspend fun stopAdvertising(): Result<Unit> = Result.Success(Unit) // no-op stub
    override suspend fun discover(): Result<Unit>        = Result.Error(Exception("NFC transport not yet implemented"))
    override suspend fun stopDiscovery(): Result<Unit>   = Result.Success(Unit) // no-op stub
    override suspend fun send(data: ByteArray): Result<Unit> = Result.Error(Exception("NFC transport not yet implemented"))
    override suspend fun sendAck(messageId: String): Result<Unit> = Result.Success(Unit) // no-op stub
    override fun receive(): Flow<ByteArray>              = emptyFlow()
    override fun getConnectedNodes(): List<TransportNode> = emptyList()
    override fun getSignalStrength(): Int                = Int.MIN_VALUE
    override fun observeState(): Flow<TransportEvent>    = emptyFlow()
}
