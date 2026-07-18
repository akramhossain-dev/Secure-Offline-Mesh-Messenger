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
 * Database Entity mapping coordinate history registries.
 *
 * Indexes:
 * - [userId] for per-user location retrieval (A33.3)
 * - [timestamp] for time-ordered sorting (A33.3)
 */
@Entity(
    tableName = "locations",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["timestamp"])
    ]
)
data class LocationEntity(
    @PrimaryKey override val entityId: String,
    val userId: String,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val accuracy: Float,
    val timestamp: Long,
    val provider: String,
    val deviceId: String
) : BaseEntity
