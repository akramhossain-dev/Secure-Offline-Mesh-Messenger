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
 * Database Entity mapping message logs.
 *
 * Indexes:
 * - [conversationId] for fast conversation query (A33.3)
 * - [timestamp] for chronological sorting (A33.3)
 */
@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["conversationId"]),
        Index(value = ["timestamp"])
    ]
)
data class MessageEntity(
    @PrimaryKey override val entityId: String,
    val messageId: String = entityId,
    val conversationId: String,
    val senderId: String,
    val senderName: String = "",
    val recipientId: String,
    val content: String,
    val timestamp: Long,
    val createdAt: Long = timestamp,
    val updatedAt: Long = timestamp,
    val edited: Boolean = false,
    val deleted: Boolean = false,
    val deliveryStatus: DbDeliveryStatus,
    val readStatus: String = "UNREAD", // "UNREAD" | "READ"
    val syncState: String = "SYNCED", // "PENDING" | "SYNCED"
    val editHistory: List<String> = emptyList(),
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
