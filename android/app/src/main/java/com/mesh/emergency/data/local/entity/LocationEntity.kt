/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mesh.emergency.core.model.BaseEntity

/**
 * Database Entity mapping user coordinates tracking log history.
 */
@Entity(tableName = "locations")
data class LocationEntity(
    @PrimaryKey override val entityId: String,
    val userId: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val timestamp: Long
) : BaseEntity
