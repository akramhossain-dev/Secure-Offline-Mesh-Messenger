/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mesh.emergency.core.model.BaseEntity

/**
 * Database Entity aggregating messages logs groupings.
 */
@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey override val entityId: String,
    val title: String,
    val lastMessageId: String?,
    val unreadCount: Int,
    val updatedAt: Long
) : BaseEntity
