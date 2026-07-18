/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mesh.emergency.core.model.BaseEntity

/**
 * Database Entity mapping discovered mesh router nodes parameters.
 *
 * Indexes:
 * - [deviceId] for fast device-based lookup (A33.3)
 * - [lastSeen] for recency sorting (A33.3)
 */
@Entity(
    tableName = "network_nodes",
    indices = [
        Index(value = ["deviceId"]),
        Index(value = ["lastSeen"])
    ]
)
data class NetworkNodeEntity(
    @PrimaryKey override val entityId: String,
    val deviceId: String,
    val nodeType: DbNodeType = DbNodeType.PHONE_NODE,
    val status: DbNodeStatus = DbNodeStatus.UNKNOWN,
    val rssi: Int = -100,
    val signalQuality: Float = 0.0f,
    val connectionType: String = "BLE",
    val batteryLevel: Int = 100,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val lastSeen: Long = System.currentTimeMillis(),
    val hopCount: Int = 1,
    val relayCapability: Boolean = true,
    val networkDistance: Int = 1
) : BaseEntity

/**
 * Node hardware categories.
 */
enum class DbNodeType {
    PHONE_NODE,
    LORA_NODE,
    RELAY_NODE,
    GATEWAY_NODE
}

/**
 * Node connectivity check state.
 */
enum class DbNodeStatus {
    ONLINE,
    OFFLINE,
    WEAK_CONNECTION,
    UNKNOWN,
    UNAVAILABLE
}
