/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.network

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
 */
@Singleton
class NetworkHealthManagerImpl @Inject constructor(
    private val localDataSource: LocalDataSource,
    private val bluetoothTransport: BluetoothTransportImpl
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

    override val networkFailureRate: StateFlow<Float> = combine(
        bluetoothTransport.packetsSentCount,
        bluetoothTransport.failedPacketsCount
    ) { sent, failed ->
        val total = sent + failed
        if (total == 0) 0.0f else failed.toFloat() / total.toFloat()
    }.stateIn(scope, SharingStarted.Eagerly, 0.0f)

    override val packetsSent: StateFlow<Int> = bluetoothTransport.packetsSentCount
    override val packetsReceived: StateFlow<Int> = bluetoothTransport.packetsReceivedCount
    override val failedPackets: StateFlow<Int> = bluetoothTransport.failedPacketsCount

    override val connectionUptime: Flow<Long> = flow {
        while (true) {
            emit(bluetoothTransport.getConnectionUptime())
            delay(1000L)
        }
    }

    override val localBatteryLevel: Int
        get() = bluetoothTransport.getLocalBatteryLevel()

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
