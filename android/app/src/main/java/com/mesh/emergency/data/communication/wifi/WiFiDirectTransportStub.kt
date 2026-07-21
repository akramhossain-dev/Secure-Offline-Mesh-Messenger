/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.communication.wifi

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
 * Extension point for a future Wi-Fi Direct transport implementation.
 *
 * Wi-Fi Direct enables high-throughput peer-to-peer communication at medium range
 * without requiring a Wi-Fi infrastructure access point.
 *
 * This stub satisfies the [Transport] interface contract and reports [TransportStatus.UNAVAILABLE].
 * When the Wi-Fi Direct transport is implemented, only this class needs to change —
 * no modifications to [CommunicationManager], [MessagingService], or any UI layer.
 *
 * Expected capabilities when implemented:
 * - [TransportCapability.BROADCAST] — group owner broadcasts to all connected peers
 * - [TransportCapability.DISCOVERABLE] — uses Wi-Fi P2P discovery APIs
 * - [TransportCapability.SIGNAL_STRENGTH] — RSSI via WifiInfo
 * - [TransportCapability.MESH_ROUTING] — relay through group owner
 */
@Singleton
class WiFiDirectTransportStub @Inject constructor() : Transport {

    override val type: TransportType = TransportType.WIFI_DIRECT

    override val capabilities: Set<TransportCapability> = setOf(
        TransportCapability.BROADCAST,
        TransportCapability.DISCOVERABLE,
        TransportCapability.SIGNAL_STRENGTH,
        TransportCapability.MESH_ROUTING
    )

    private val _status = MutableStateFlow(TransportStatus.UNAVAILABLE)
    override val status: StateFlow<TransportStatus> = _status.asStateFlow()

    override suspend fun start(): Result<Unit>           = Result.Success(Unit) // no-op stub
    override suspend fun stop(): Result<Unit>            = Result.Success(Unit) // no-op stub
    override suspend fun connect(): Result<Unit>         = Result.Error(Exception("Wi-Fi Direct transport not yet implemented"))
    override suspend fun disconnect(): Result<Unit>      = Result.Success(Unit) // no-op stub
    override suspend fun advertise(): Result<Unit>       = Result.Error(Exception("Wi-Fi Direct transport not yet implemented"))
    override suspend fun stopAdvertising(): Result<Unit> = Result.Success(Unit) // no-op stub
    override suspend fun discover(): Result<Unit>        = Result.Error(Exception("Wi-Fi Direct transport not yet implemented"))
    override suspend fun stopDiscovery(): Result<Unit>   = Result.Success(Unit) // no-op stub
    override suspend fun send(data: ByteArray): Result<Unit> = Result.Error(Exception("Wi-Fi Direct transport not yet implemented"))
    override suspend fun sendAck(messageId: String): Result<Unit> = Result.Success(Unit) // no-op stub
    override fun receive(): Flow<ByteArray>              = emptyFlow()
    override fun getConnectedNodes(): List<TransportNode> = emptyList()
    override fun getSignalStrength(): Int                = Int.MIN_VALUE
    override fun observeState(): Flow<TransportEvent>    = emptyFlow()
}
