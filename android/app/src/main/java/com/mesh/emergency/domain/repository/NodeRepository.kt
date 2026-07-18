/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.domain.repository

import com.mesh.emergency.core.common.result.Result
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface regulating known mesh router nodes.
 */
interface NodeRepository {
    /** Stream of all registered mesh network nodes. */
    fun getNetworkNodes(): Flow<Result<List<NodeDomainModel>>>

    /** Caches a node record. */
    suspend fun saveNode(node: NodeDomainModel): Result<Unit>

    /** Deletes a node record by its identifier. */
    suspend fun deleteNode(nodeId: String): Result<Unit>
}

/**
 * Domain model representing a mesh network routing node.
 */
data class NodeDomainModel(
    val id: String,
    val deviceId: String,
    val type: String,
    val status: String,
    val rssi: Int,
    val lastSeen: Long,
    val batteryLevel: Int,
    val latitude: Double,
    val longitude: Double,
    val hopCount: Int,
    val relayCapability: Boolean,
    val networkDistance: Int
)
