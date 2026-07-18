/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mesh.emergency.core.model.BaseEntity

/**
 * Database Entity representing local or bridged device identities.
 */
@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey override val entityId: String,
    val deviceName: String,
    val deviceType: String,
    val platformInfo: String,
    val createdTime: Long,
    val lastActiveTime: Long
) : BaseEntity
