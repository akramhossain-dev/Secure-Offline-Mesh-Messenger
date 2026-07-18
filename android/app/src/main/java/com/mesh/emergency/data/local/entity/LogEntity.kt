/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mesh.emergency.core.model.BaseEntity

/**
 * Database Entity mapping local diagnostic logs.
 */
@Entity(tableName = "logs")
data class LogEntity(
    @PrimaryKey override val entityId: String,
    val level: String,
    val category: String,
    val message: String,
    val timestamp: Long,
    val deviceId: String,
    val moduleName: String,
    val stackTrace: String? = null
) : BaseEntity
