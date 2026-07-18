/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mesh.emergency.core.model.BaseEntity

/**
 * Database Entity representing a paired peer contact or local user.
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey override val entityId: String,
    val username: String,
    val publicKey: String,
    val avatarUrl: String?,
    val isCurrentUser: Boolean,
    val lastSeen: Long
) : BaseEntity
