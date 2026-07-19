/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.communication

import com.mesh.emergency.core.common.result.Result
import com.mesh.emergency.core.communication.CommunicationManager
import com.mesh.emergency.core.communication.CommunicationState
import com.mesh.emergency.core.communication.DeliveryResult
import com.mesh.emergency.core.communication.Transport
import com.mesh.emergency.core.communication.TransportStatus
import com.mesh.emergency.core.communication.TransportType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import com.mesh.emergency.core.domain.AppStateRepository
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [CommunicationManager] selecting active transceivers by priority.
 * Priority hierarchy:
 * 1. Bluetooth BLE (high throughput, short range)
 * 2. LoRa Mesh (low throughput, long range)
 * 3. Mock loopback fallback
 */
@Singleton
class CommunicationManagerImpl @Inject constructor(
    private val bluetoothTransport: com.mesh.emergency.data.communication.bluetooth.BluetoothTransportImpl,
    private val appStateRepository: AppStateRepository
) : CommunicationManager {

    private val transports = mutableMapOf<TransportType, Transport>()

    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)

    private val _activeTransport = MutableStateFlow<Transport?>(null)
    override val activeTransport: StateFlow<Transport?> = _activeTransport.asStateFlow()

    private val _communicationState = MutableStateFlow(CommunicationState.DISCONNECTED)
    override val communicationState: StateFlow<CommunicationState> = _communicationState.asStateFlow()

    init {
        registerTransport(bluetoothTransport)
        scope.launch {
            while (true) {
                try {
                    val status = bluetoothTransport.status.value
                    if (status == com.mesh.emergency.core.communication.TransportStatus.DISCONNECTED ||
                        status == com.mesh.emergency.core.communication.TransportStatus.UNAVAILABLE) {
                        timber.log.Timber.d("CommunicationManager: Attempting to connect Bluetooth transport...")
                        bluetoothTransport.connect()
                    }
                } catch (e: Exception) {
                    timber.log.Timber.e(e, "CommunicationManager: Failed to connect Bluetooth transport")
                }
                kotlinx.coroutines.delay(15000L)
            }
        }
    }

    override fun registerTransport(transport: Transport) {
        synchronized(transports) {
            transports[transport.type] = transport
            evaluateActiveTransport()
        }
        scope.launch {
            transport.status.collect {
                synchronized(transports) {
                    evaluateActiveTransport()
                }
            }
        }
    }

    override fun unregisterTransport(type: TransportType) {
        synchronized(transports) {
            transports.remove(type)
            evaluateActiveTransport()
        }
    }

    override fun getTransports(): List<Transport> {
        return synchronized(transports) { transports.values.toList() }
    }

    override fun getAvailableTransports(): List<TransportType> {
        return synchronized(transports) {
            transports.filter { it.value.status.value != TransportStatus.UNAVAILABLE }
                .map { it.key }
        }
    }

    override suspend fun sendMessage(data: ByteArray): Result<DeliveryResult> {
        val transport = _activeTransport.value
            ?: return Result.Error(Exception("No active transport channels available"))

        if (transport.status.value != TransportStatus.CONNECTED) {
            return Result.Error(Exception("Active transport is not in connected state"))
        }

        _communicationState.value = CommunicationState.SENDING
        val result = transport.send(data)
        return when (result) {
            is Result.Success -> {
                _communicationState.value = CommunicationState.CONNECTED
                Result.Success(DeliveryResult.DELIVERED)
            }
            is Result.Error -> {
                _communicationState.value = CommunicationState.FAILED
                Result.Error(result.exception)
            }
            is Result.Loading -> {
                Result.Loading
            }
        }
    }

    override fun receiveMessages(): Flow<ByteArray> {
        return _activeTransport.flatMapLatest { transport ->
            transport?.receive() ?: emptyFlow()
        }
    }

    /**
     * Evaluates registered transceivers status and sets the active channel using priority.
     */
    private fun evaluateActiveTransport() {
        val sortedList = transports.values.sortedBy {
            when (it.type) {
                TransportType.BLUETOOTH -> 1
                TransportType.LORA      -> 2
                TransportType.MOCK      -> 3
            }
        }

        // Select the first connected, or connecting, or available transceiver in priority order
        val selected = sortedList.firstOrNull { it.status.value == TransportStatus.CONNECTED }
            ?: sortedList.firstOrNull { it.status.value == TransportStatus.CONNECTING }
            ?: sortedList.firstOrNull { it.status.value == TransportStatus.DISCONNECTED }

        _activeTransport.value = selected

        _communicationState.value = when (selected?.status?.value) {
            TransportStatus.CONNECTED    -> CommunicationState.CONNECTED
            TransportStatus.CONNECTING   -> CommunicationState.SEARCHING
            TransportStatus.DISCONNECTED -> CommunicationState.DISCONNECTED
            TransportStatus.UNAVAILABLE  -> CommunicationState.UNAVAILABLE
            null                         -> CommunicationState.UNAVAILABLE
        }

        // Sync connection state to the central AppStateRepository
        val isOnline = selected?.status?.value == TransportStatus.CONNECTED
        val transportLabel = selected?.type?.name ?: "NONE"
        val nodeCount = if (isOnline) 1 else 0
        appStateRepository.updateConnectionStatus(isOnline, transportLabel, nodeCount)
    }
}
