/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mesh.emergency.core.model.BaseEntity

/**
 * Database Entity representing local user profiles or contact peers list logs.
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey override val entityId: String,
    val username: String,
    val profileImageRef: String?,
    val languagePreference: String,
    val createdTime: Long,
    val updatedTime: Long,
    val status: String,
    val isCurrentUser: Boolean,
    val lastSeen: Long,
    // Contact-specific properties (for peer relationships)
    val trustedStatus: Boolean = false,
    val nickname: String? = null,
    val publicKey: String? = null
) : BaseEntity
