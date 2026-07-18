/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.communication

import com.mesh.emergency.core.common.result.Result
import com.mesh.emergency.core.communication.Transport
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
 * Stub representing a future Bluetooth BLE transport pathway.
 */
@Singleton
class BluetoothTransportStub @Inject constructor() : Transport {

    override val type: TransportType = TransportType.BLUETOOTH

    private val _status = MutableStateFlow(TransportStatus.DISCONNECTED)
    override val status: StateFlow<TransportStatus> = _status.asStateFlow()

    override suspend fun connect(): Result<Unit> {
        _status.value = TransportStatus.CONNECTING
        _status.value = TransportStatus.CONNECTED
        return Result.Success(Unit)
    }

    override suspend fun disconnect(): Result<Unit> {
        _status.value = TransportStatus.DISCONNECTED
        return Result.Success(Unit)
    }

    override suspend fun send(data: ByteArray): Result<Unit> {
        return Result.Error(Exception("Bluetooth transport features are stubbed"))
    }

    override fun receive(): Flow<ByteArray> = emptyFlow()
}
