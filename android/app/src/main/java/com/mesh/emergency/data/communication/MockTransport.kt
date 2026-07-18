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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loopback transceiver implementation of [Transport] used in tests and mock states.
 */
@Singleton
class MockTransport @Inject constructor() : Transport {

    override val type: TransportType = TransportType.MOCK

    private val _status = MutableStateFlow(TransportStatus.DISCONNECTED)
    override val status: StateFlow<TransportStatus> = _status.asStateFlow()

    private val _receiveFlow = MutableSharedFlow<ByteArray>(extraBufferCapacity = 1)

    override suspend fun connect(): Result<Unit> {
        _status.value = TransportStatus.CONNECTED
        return Result.Success(Unit)
    }

    override suspend fun disconnect(): Result<Unit> {
        _status.value = TransportStatus.DISCONNECTED
        return Result.Success(Unit)
    }

    override suspend fun send(data: ByteArray): Result<Unit> {
        // Loopback design: emitted back into receiver flow.
        _receiveFlow.tryEmit(data)
        return Result.Success(Unit)
    }

    override fun receive(): Flow<ByteArray> = _receiveFlow.asSharedFlow()

    /** Modifies status programmatically in tests. */
    fun setMockStatus(newStatus: TransportStatus) {
        _status.value = newStatus
    }
}
