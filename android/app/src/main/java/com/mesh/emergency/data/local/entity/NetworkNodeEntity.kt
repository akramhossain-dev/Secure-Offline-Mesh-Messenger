/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mesh.emergency.core.model.BaseEntity

/**
 * Database Entity mapping mesh nodes diagnostics metrics.
 */
@Entity(tableName = "network_nodes")
data class NetworkNodeEntity(
    @PrimaryKey override val entityId: String,
    val nodeName: String,
    val rssi: Int,
    val hops: Int,
    val batteryLevel: Float,
    val lastSeen: Long
) : BaseEntity
