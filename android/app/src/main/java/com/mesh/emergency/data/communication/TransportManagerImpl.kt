/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.communication

import com.mesh.emergency.core.common.result.Result
import com.mesh.emergency.core.communication.CommunicationManager
import com.mesh.emergency.core.communication.CommunicationState
import com.mesh.emergency.core.communication.ConnectionState
import com.mesh.emergency.core.communication.TransportManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real implementation of [TransportManager] delegating all data transfer and state to
 * the existing [CommunicationManager] (which manages BLE and LoRa transports).
 *
 * Replaces [com.mesh.emergency.core.communication.TransportManagerStub] in production DI binding.
 */
@Singleton
class TransportManagerImpl @Inject constructor(
    private val communicationManager: CommunicationManager
) : TransportManager {

    private val scope = CoroutineScope(Dispatchers.Default)

    override val isTransportAvailable: StateFlow<Boolean> =
        communicationManager.communicationState
            .map { state -> state == CommunicationState.CONNECTED || state == CommunicationState.SENDING }
            .stateIn(scope, SharingStarted.Eagerly, false)

    override val connectionState: StateFlow<ConnectionState> =
        communicationManager.communicationState
            .map { state ->
                when (state) {
                    CommunicationState.CONNECTED,
                    CommunicationState.SENDING,
                    CommunicationState.RECEIVING    -> ConnectionState.CONNECTED
                    CommunicationState.SEARCHING    -> ConnectionState.CONNECTING
                    CommunicationState.DISCONNECTED -> ConnectionState.DISCONNECTED
                    CommunicationState.FAILED       -> ConnectionState.RECONNECTING
                    CommunicationState.UNAVAILABLE  -> ConnectionState.DISCONNECTED
                }
            }
            .stateIn(scope, SharingStarted.Eagerly, ConnectionState.DISCONNECTED)

    override suspend fun sendData(data: ByteArray): Result<Unit> {
        val result = communicationManager.sendMessage(data)
        return when (result) {
            is Result.Success -> Result.Success(Unit)
            is Result.Error   -> Result.Error(result.exception)
            is Result.Loading -> Result.Loading
        }
    }
}
