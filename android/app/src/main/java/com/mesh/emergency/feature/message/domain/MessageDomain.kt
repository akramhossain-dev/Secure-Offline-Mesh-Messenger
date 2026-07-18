/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.feature.message.domain

import com.mesh.emergency.data.local.entity.DbDeliveryStatus
import com.mesh.emergency.data.local.entity.DbMessagePriority
import com.mesh.emergency.data.local.entity.DbMessageType
import com.mesh.emergency.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

// ─────────────────────────────────────────────────────────────────────────────
// Messaging Domain Models
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Presentation-layer message model, decoupled from Room entities.
 */
data class Message(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val recipientId: String,
    val content: String,
    val timestamp: Long,
    val deliveryStatus: DbDeliveryStatus,
    val type: DbMessageType,
    val priority: DbMessagePriority,
    val retryCount: Int,
    val expiryTime: Long,
    val isSelf: Boolean = senderId == "self"
)

/**
 * Conversation summary shown in the conversation list.
 */
data class ConversationSummary(
    val id: String,
    val title: String,
    val lastMessagePreview: String,
    val unreadCount: Int,
    val updatedAt: Long,
    val isOnline: Boolean = false
)

// ─────────────────────────────────────────────────────────────────────────────
// Repository Interface
// ─────────────────────────────────────────────────────────────────────────────

interface MessageRepository {
    fun getConversations(): Flow<List<ConversationSummary>>
    fun getMessagesForConversation(conversationId: String): Flow<List<Message>>
    suspend fun sendMessage(message: Message)
    suspend fun deleteMessage(messageId: String)
    suspend fun getMessageById(messageId: String): Message?
}

// ─────────────────────────────────────────────────────────────────────────────
// Mappers
// ─────────────────────────────────────────────────────────────────────────────

fun MessageEntity.toDomain() = Message(
    id             = entityId,
    conversationId = conversationId,
    senderId       = senderId,
    recipientId    = recipientId,
    content        = content,
    timestamp      = timestamp,
    deliveryStatus = deliveryStatus,
    type           = type,
    priority       = priority,
    retryCount     = retryCount,
    expiryTime     = expiryTime
)

fun Message.toEntity() = MessageEntity(
    entityId       = id,
    conversationId = conversationId,
    senderId       = senderId,
    recipientId    = recipientId,
    content        = content,
    timestamp      = timestamp,
    deliveryStatus = deliveryStatus,
    type           = type,
    priority       = priority,
    retryCount     = retryCount,
    expiryTime     = expiryTime
)
