/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.network

import com.mesh.emergency.core.communication.CommunicationManager
import com.mesh.emergency.core.communication.TransportType
import com.mesh.emergency.core.network.NetworkHealthManager
import com.mesh.emergency.data.local.LocalDataSource
import com.mesh.emergency.data.local.entity.DbNodeStatus
import com.mesh.emergency.data.communication.bluetooth.BluetoothTransportImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [NetworkHealthManager] tracking active statistics of mesh connectivity.
 *
 * Packet statistics ([packetsSent], [packetsReceived], [failedPackets]) are sourced from
 * the active transport via [CommunicationManager] if it is a [BluetoothTransportImpl],
 * allowing these metrics to be extended to other transports in the future.
 *
 * Node availability is tracked from the local database, making it transport-agnostic.
 */
@Singleton
class NetworkHealthManagerImpl @Inject constructor(
    private val localDataSource: LocalDataSource,
    private val communicationManager: CommunicationManager
) : NetworkHealthManager {

    private val scope = CoroutineScope(Dispatchers.IO)

    override val availableNodesCount: StateFlow<Int> = localDataSource.getNetworkNodes()
        .map { list ->
            val activeCount = list.count { it.status == DbNodeStatus.ONLINE || it.status == DbNodeStatus.WEAK_CONNECTION }
            if (activeCount > 0) activeCount + 1 else 0
        }
        .stateIn(scope, SharingStarted.Eagerly, 0)

    override val activeConnectionsCount: StateFlow<Int> = localDataSource.getNetworkNodes()
        .map { list ->
            list.count { it.status == DbNodeStatus.ONLINE || it.status == DbNodeStatus.WEAK_CONNECTION }
        }
        .stateIn(scope, SharingStarted.Eagerly, 0)

    override val averageSignalQuality: StateFlow<Float> = localDataSource.getNetworkNodes()
        .map { list ->
            val active = list.filter { it.status == DbNodeStatus.ONLINE || it.status == DbNodeStatus.WEAK_CONNECTION }
            if (active.isEmpty()) 100.0f
            else active.map { it.signalQuality }.average().toFloat()
        }
        .stateIn(scope, SharingStarted.Eagerly, 100.0f)

    // ── Packet Stats: sourced from active transport when available ────────────

    private fun activeBluetoothTransport(): BluetoothTransportImpl? {
        val active = communicationManager.activeTransport.value
        return if (active?.type == TransportType.BLUETOOTH) active as? BluetoothTransportImpl else null
    }

    override val networkFailureRate: StateFlow<Float> = combine(
        communicationManager.communicationState,
        communicationManager.activeTransport
    ) { _, _ ->
        val bt = activeBluetoothTransport()
        if (bt != null) {
            val sent = bt.packetsSentCount.value
            val failed = bt.failedPacketsCount.value
            val total = sent + failed
            if (total == 0) 0.0f else failed.toFloat() / total.toFloat()
        } else {
            0.0f
        }
    }.stateIn(scope, SharingStarted.Eagerly, 0.0f)

    override val packetsSent: StateFlow<Int> = communicationManager.activeTransport
        .map { active ->
            if (active?.type == TransportType.BLUETOOTH) {
                (active as? BluetoothTransportImpl)?.packetsSentCount?.value ?: 0
            } else 0
        }
        .stateIn(scope, SharingStarted.Eagerly, 0)

    override val packetsReceived: StateFlow<Int> = communicationManager.activeTransport
        .map { active ->
            if (active?.type == TransportType.BLUETOOTH) {
                (active as? BluetoothTransportImpl)?.packetsReceivedCount?.value ?: 0
            } else 0
        }
        .stateIn(scope, SharingStarted.Eagerly, 0)

    override val failedPackets: StateFlow<Int> = communicationManager.activeTransport
        .map { active ->
            if (active?.type == TransportType.BLUETOOTH) {
                (active as? BluetoothTransportImpl)?.failedPacketsCount?.value ?: 0
            } else 0
        }
        .stateIn(scope, SharingStarted.Eagerly, 0)

    override val connectionUptime: Flow<Long> = flow {
        while (true) {
            emit(activeBluetoothTransport()?.getConnectionUptime() ?: 0L)
            delay(1000L)
        }
    }

    override val localBatteryLevel: Int
        get() = activeBluetoothTransport()?.getLocalBatteryLevel() ?: -1

    override fun recordFailure() {
        // No-op: handled directly by packet transmission handlers in BLE transport
    }

    override fun recordSuccess() {
        // No-op: handled directly by packet transmission handlers in BLE transport
    }

    override fun updateCounts(nodes: Int, connections: Int, signal: Float) {
        // No-op: handled dynamically from database state and packet flow
    }
}
