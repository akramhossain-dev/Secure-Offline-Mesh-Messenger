/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mesh.emergency.core.model.BaseEntity

/**
 * Database Entity representing active mesh hardware bridges (ESP32/LoRa).
 */
@Entity(tableName = "network_nodes")
data class NetworkNodeEntity(
    @PrimaryKey override val entityId: String,
    val nodeName: String,
    val nodeType: String,
    val status: String,
    val lastSeen: Long,
    val signalPlaceholder: String?
) : BaseEntity
