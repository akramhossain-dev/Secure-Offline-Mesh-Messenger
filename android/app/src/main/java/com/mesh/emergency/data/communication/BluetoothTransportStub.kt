/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.communication

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
 * @deprecated This stub is superseded by [com.mesh.emergency.data.communication.bluetooth.BluetoothTransportImpl].
 * Retained for compilation compatibility during the transport architecture migration.
 * Do not inject or use this class — use [BluetoothTransportImpl] via [CommunicationModule] instead.
 */
@Singleton
@Deprecated("Use BluetoothTransportImpl via CommunicationModule instead")
class BluetoothTransportStub @Inject constructor() : Transport {

    override val type: TransportType = TransportType.BLUETOOTH

    override val capabilities: Set<TransportCapability> = emptySet()

    private val _status = MutableStateFlow(TransportStatus.UNAVAILABLE)
    override val status: StateFlow<TransportStatus> = _status.asStateFlow()

    override suspend fun start(): Result<Unit>           = Result.Success(Unit)
    override suspend fun stop(): Result<Unit>            = Result.Success(Unit)
    override suspend fun connect(): Result<Unit>         = Result.Error(Exception("Use BluetoothTransportImpl instead"))
    override suspend fun disconnect(): Result<Unit>      = Result.Success(Unit)
    override suspend fun advertise(): Result<Unit>       = Result.Error(Exception("Use BluetoothTransportImpl instead"))
    override suspend fun stopAdvertising(): Result<Unit> = Result.Success(Unit)
    override suspend fun discover(): Result<Unit>        = Result.Error(Exception("Use BluetoothTransportImpl instead"))
    override suspend fun stopDiscovery(): Result<Unit>   = Result.Success(Unit)
    override suspend fun send(data: ByteArray): Result<Unit> = Result.Error(Exception("Use BluetoothTransportImpl instead"))
    override suspend fun sendAck(messageId: String): Result<Unit> = Result.Success(Unit)
    override fun receive(): Flow<ByteArray>              = emptyFlow()
    override fun getConnectedNodes(): List<TransportNode> = emptyList()
    override fun getSignalStrength(): Int                = Int.MIN_VALUE
    override fun observeState(): Flow<TransportEvent>    = emptyFlow()
}
