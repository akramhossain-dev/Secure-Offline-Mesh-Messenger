/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mesh.emergency.core.model.BaseEntity

/**
 * Database Entity mapping message logs.
 */
@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey override val entityId: String,
    val conversationId: String,
    val senderId: String,
    val recipientId: String,
    val content: String,
    val timestamp: Long,
    val deliveryStatus: DbDeliveryStatus,
    val type: DbMessageType,
    val priority: DbMessagePriority,
    val expiryTime: Long,
    val retryCount: Int
) : BaseEntity

/**
 * Maps message delivery status logs to Room.
 */
enum class DbDeliveryStatus {
    PENDING,
    QUEUED,
    SENDING,
    SENT,
    DELIVERED,
    FAILED,
    EXPIRED
}

/**
 * Maps message types logs to Room.
 */
enum class DbMessageType {
    TEXT,
    VOICE,
    LOCATION,
    SYSTEM
}

/**
 * Maps message priority thresholds to Room.
 */
enum class DbMessagePriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}
