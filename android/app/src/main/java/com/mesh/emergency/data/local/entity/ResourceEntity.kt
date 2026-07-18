/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mesh.emergency.core.model.BaseEntity

/**
 * Database Entity mapping active resource supplies (e.g. food, water, medicine, power).
 */
@Entity(tableName = "resources")
data class ResourceEntity(
    @PrimaryKey override val entityId: String,
    val title: String,
    val description: String,
    val type: String,
    val quantity: Int,
    val contactInfo: String,
    val timestamp: Long
) : BaseEntity
