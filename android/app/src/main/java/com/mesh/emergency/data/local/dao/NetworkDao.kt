/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mesh.emergency.data.local.entity.NetworkNodeEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object regulating queries targeting the 'network_nodes' table.
 */
@Dao
interface NetworkDao {

    /** Streams diagnostic data for all nodes. */
    @Query("SELECT * FROM network_nodes ORDER BY lastSeen DESC")
    fun getNetworkNodes(): Flow<List<NetworkNodeEntity>>

    /** Queries node metrics by network identifier. */
    @Query("SELECT * FROM network_nodes WHERE entityId = :nodeId LIMIT 1")
    suspend fun getNodeById(nodeId: String): NetworkNodeEntity?

    /** Insert or replace node telemetry logs. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNode(node: NetworkNodeEntity)

    /** Delete node telemetry logs. */
    @Delete
    suspend fun deleteNode(node: NetworkNodeEntity)

    /** Get only active (online/weak) nodes using indexed status lookup (A33.3). */
    @Query("SELECT * FROM network_nodes WHERE status IN ('ONLINE', 'WEAK_CONNECTION') ORDER BY lastSeen DESC")
    fun getActiveNodes(): Flow<List<NetworkNodeEntity>>

    /** Get by device ID using index (A33.3). */
    @Query("SELECT * FROM network_nodes WHERE deviceId = :deviceId LIMIT 1")
    suspend fun getNodeByDeviceId(deviceId: String): NetworkNodeEntity?
}
