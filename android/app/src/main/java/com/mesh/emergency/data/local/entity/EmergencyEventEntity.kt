/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mesh.emergency.core.model.BaseEntity

/**
 * Database Entity mapping LoRa emergency alerts.
 */
@Entity(tableName = "emergency_events")
data class EmergencyEventEntity(
    @PrimaryKey override val entityId: String,
    val senderId: String,
    val latitude: Double,
    val longitude: Double,
    val message: String,
    val timestamp: Long,
    val isResolved: Boolean
) : BaseEntity
