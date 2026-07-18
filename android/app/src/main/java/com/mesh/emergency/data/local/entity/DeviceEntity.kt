/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mesh.emergency.core.model.BaseEntity

/**
 * Database Entity representing a discovered bluetooth hardware device or paired bridge.
 */
@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey override val entityId: String,
    val name: String,
    val macAddress: String,
    val rssi: Int,
    val lastSeen: Long
) : BaseEntity
