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
import com.mesh.emergency.core.communication.TransportPriority
import com.mesh.emergency.core.communication.TransportStatus
import com.mesh.emergency.core.communication.TransportType
import com.mesh.emergency.core.domain.AppStateRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [CommunicationManager] selecting the active transport by priority.
 *
 * Priority is determined by [TransportPriority]: Bluetooth > LoRa > Wi-Fi Direct > NFC > USB > Mock.
 * Only [TransportStatus.CONNECTED] transports are eligible as the active sender.
 *
 * Adding a new transport technology requires:
 * 1. A class implementing [Transport]
 * 2. A `@Binds @IntoSet` binding in the DI module
 * No changes to this class, [MessagingService], or any UI layer.
 *
 * The reconnect loop iterates over ALL registered transports and attempts reconnection
 * for any that are [TransportStatus.DISCONNECTED] — not just Bluetooth.
 */
@Singleton
class CommunicationManagerImpl @Inject constructor(
    /** All registered Transport implementations, injected via Hilt @IntoSet multi-bindings. */
    private val registeredTransports: Set<@JvmSuppressWildcards Transport>,
    private val appStateRepository: AppStateRepository
) : CommunicationManager {

    private val transports = mutableMapOf<TransportType, Transport>()
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _activeTransport = MutableStateFlow<Transport?>(null)
    override val activeTransport: StateFlow<Transport?> = _activeTransport.asStateFlow()

    private val _communicationState = MutableStateFlow(CommunicationState.DISCONNECTED)
    override val communicationState: StateFlow<CommunicationState> = _communicationState.asStateFlow()

    init {
        // Register all injected transports into the internal map
        registeredTransports.forEach { registerTransport(it) }

        // Generic reconnect loop — iterates all transports, not just Bluetooth
        scope.launch {
            while (true) {
                try {
                    synchronized(transports) { transports.values.toList() }.forEach { transport ->
                        val status = transport.status.value
                        if (status == TransportStatus.DISCONNECTED) {
                            Timber.d("CommunicationManager: Attempting to reconnect ${transport.type}...")
                            transport.connect()
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "CommunicationManager: Reconnect loop error")
                }
                delay(15_000L)
            }
        }
    }

    override fun registerTransport(transport: Transport) {
        synchronized(transports) {
            transports[transport.type] = transport
            evaluateActiveTransport()
        }
        // Observe status changes so the active transport is re-evaluated immediately on state change
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
            return Result.Error(Exception("Active transport '${transport.type}' is not in connected state"))
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
            is Result.Loading -> Result.Loading
        }
    }

    /**
     * Receives all inbound byte payloads from ALL registered transports, not just the active one.
     * This ensures we don't miss packets that arrive on a secondary transport while the active
     * transport changes during a switchover.
     */
    override fun receiveMessages(): Flow<ByteArray> {
        return synchronized(transports) {
            if (transports.isEmpty()) emptyFlow()
            else transports.values.map { it.receive() }.merge()
        }
    }

    /**
     * Selects the highest-priority CONNECTED transport as the active sender.
     * Priority ordering is defined by [TransportPriority] — adding new transports
     * only requires adding values to that enum, not modifying this logic.
     *
     * Selection order:
     * 1. CONNECTED transport with lowest [TransportPriority.ordinal]
     * 2. CONNECTING transport (fallback while connecting)
     * 3. DISCONNECTED transport (last resort)
     * UNAVAILABLE transports are never selected.
     */
    private fun evaluateActiveTransport() {
        val sortedList = transports.values.sortedBy { TransportPriority.of(it.type) }

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
        val isOnline     = selected?.status?.value == TransportStatus.CONNECTED
        val nodeCount    = if (isOnline) selected?.getConnectedNodes()?.size?.coerceAtLeast(1) ?: 1 else 0
        val transportLabel = selected?.type?.name ?: "NONE"
        appStateRepository.updateConnectionStatus(isOnline, transportLabel, nodeCount)
    }
}
