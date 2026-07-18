/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.network

import com.mesh.emergency.core.common.result.Result
import com.mesh.emergency.core.network.NetworkEvent
import com.mesh.emergency.core.network.NodeDiscoveryManager
import com.mesh.emergency.data.local.LocalDataSource
import com.mesh.emergency.data.local.entity.DbNodeStatus
import com.mesh.emergency.data.local.entity.NetworkNodeEntity
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [NodeDiscoveryManager] notifying listeners of topology adjustments.
 */
@Singleton
class NodeDiscoveryManagerImpl @Inject constructor(
    private val localDataSource: LocalDataSource
) : NodeDiscoveryManager {

    private val _networkEvents = MutableSharedFlow<NetworkEvent>(extraBufferCapacity = 64)
    override val networkEvents: SharedFlow<NetworkEvent> = _networkEvents.asSharedFlow()

    override suspend fun discoverNode(node: NetworkNodeEntity): Result<Unit> {
        return try {
            localDataSource.insertNode(node)
            _networkEvents.tryEmit(NetworkEvent.NodeDiscovered(node.entityId))
            _networkEvents.tryEmit(NetworkEvent.SignalChanged(node.entityId, node.rssi))
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun removeNode(nodeId: String): Result<Unit> {
        return try {
            val match = localDataSource.getNodeById(nodeId)
            if (match != null) {
                localDataSource.deleteNode(match)
                _networkEvents.tryEmit(NetworkEvent.NodeLost(nodeId))
                Result.Success(Unit)
            } else {
                Result.Error(Exception("Node not found"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun updateNodeStatus(nodeId: String, status: String): Result<Unit> {
        return try {
            val match = localDataSource.getNodeById(nodeId)
            if (match != null) {
                val dbStatus = DbNodeStatus.valueOf(status)
                val updated = match.copy(status = dbStatus)
                localDataSource.insertNode(updated)

                val isConnected = dbStatus == DbNodeStatus.ONLINE || dbStatus == DbNodeStatus.WEAK_CONNECTION
                _networkEvents.tryEmit(NetworkEvent.ConnectionChanged(nodeId, isConnected))
                Result.Success(Unit)
            } else {
                Result.Error(Exception("Node not found"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
