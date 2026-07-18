/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.repository

import com.mesh.emergency.core.common.result.Result
import com.mesh.emergency.data.local.LocalDataSource
import com.mesh.emergency.data.local.entity.DbNodeStatus
import com.mesh.emergency.data.local.entity.DbNodeType
import com.mesh.emergency.data.local.entity.NetworkNodeEntity
import com.mesh.emergency.domain.repository.NodeDomainModel
import com.mesh.emergency.domain.repository.NodeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [NodeRepository] mapping Room database logs.
 */
@Singleton
class NodeRepositoryImpl @Inject constructor(
    private val localDataSource: LocalDataSource
) : NodeRepository {

    override fun getNetworkNodes(): Flow<Result<List<NodeDomainModel>>> {
        return localDataSource.getNetworkNodes().map { entities ->
            Result.Success(entities.map { it.toDomain() })
        }
    }

    override suspend fun saveNode(node: NodeDomainModel): Result<Unit> {
        return try {
            val entity = NetworkNodeEntity(
                entityId = node.id,
                deviceId = node.deviceId,
                nodeType = DbNodeType.valueOf(node.type),
                status = DbNodeStatus.valueOf(node.status),
                rssi = node.rssi,
                lastSeen = node.lastSeen,
                batteryLevel = node.batteryLevel,
                latitude = node.latitude,
                longitude = node.longitude,
                hopCount = node.hopCount,
                relayCapability = node.relayCapability,
                networkDistance = node.networkDistance
            )
            localDataSource.insertNode(entity)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun deleteNode(nodeId: String): Result<Unit> {
        return try {
            val list = localDataSource.getNodeById(nodeId)
            if (list != null) {
                localDataSource.deleteNode(list)
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}

private fun NetworkNodeEntity.toDomain(): NodeDomainModel = NodeDomainModel(
    id = entityId,
    deviceId = deviceId,
    type = nodeType.name,
    status = status.name,
    rssi = rssi,
    lastSeen = lastSeen,
    batteryLevel = batteryLevel,
    latitude = latitude,
    longitude = longitude,
    hopCount = hopCount,
    relayCapability = relayCapability,
    networkDistance = networkDistance
)
