/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mesh.emergency.core.model.BaseEntity

/**
 * Database Entity mapping recorded voice message metadata.
 */
@Entity(tableName = "voice_messages")
data class VoiceMessageEntity(
    @PrimaryKey override val entityId: String,
    val senderId: String,
    val receiverId: String,
    val fileReference: String,
    val duration: Long,
    val fileSize: Long,
    val format: String,
    val quality: String,
    val timestamp: Long,
    val status: String
) : BaseEntity
