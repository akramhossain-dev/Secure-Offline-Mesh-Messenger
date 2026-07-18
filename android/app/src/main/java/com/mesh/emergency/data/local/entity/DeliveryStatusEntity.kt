/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mesh.emergency.core.model.BaseEntity

/**
 * Database Entity representing packet delivery validation trails per node hop.
 */
@Entity(tableName = "delivery_statuses")
data class DeliveryStatusEntity(
    @PrimaryKey override val entityId: String,
    val messageId: String,
    val nodeId: String,
    val status: DbDeliveryStatus,
    val timestamp: Long
) : BaseEntity
