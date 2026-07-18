/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.network

import com.mesh.emergency.core.common.result.Result
import com.mesh.emergency.data.local.entity.NetworkNodeEntity
import kotlinx.coroutines.flow.SharedFlow

/**
 * Interface contract representing offline mesh routing network awareness event managers.
 */
interface NodeDiscoveryManager {
    /** Exposes mesh network events (discovered nodes, signal changes, lost connections). */
    val networkEvents: SharedFlow<NetworkEvent>

    /** Caches discovered node properties. */
    suspend fun discoverNode(node: NetworkNodeEntity): Result<Unit>

    /** Removes a node by ID. */
    suspend fun removeNode(nodeId: String): Result<Unit>

    /** Modifies connectivity status checks. */
    suspend fun updateNodeStatus(nodeId: String, status: String): Result<Unit>
}

/**
 * Mesh network topology event payloads.
 */
sealed class NetworkEvent {
    data class NodeDiscovered(val nodeId: String) : NetworkEvent()
    data class NodeLost(val nodeId: String) : NetworkEvent()
    data class ConnectionChanged(val nodeId: String, val isConnected: Boolean) : NetworkEvent()
    data class SignalChanged(val nodeId: String, val rssi: Int) : NetworkEvent()
}
