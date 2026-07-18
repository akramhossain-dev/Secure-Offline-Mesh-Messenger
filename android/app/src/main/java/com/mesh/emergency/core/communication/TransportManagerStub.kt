/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.communication

import com.mesh.emergency.core.common.result.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Temporary communications stub used to build core architecture compilation targets.
 */
@Singleton
class TransportManagerStub @Inject constructor() : TransportManager {

    private val _isTransportAvailable = MutableStateFlow(false)
    override val isTransportAvailable: StateFlow<Boolean> = _isTransportAvailable.asStateFlow()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    override suspend fun sendData(data: ByteArray): Result<Unit> {
        return Result.Error(Exception("Transport not implemented yet"))
    }
}
