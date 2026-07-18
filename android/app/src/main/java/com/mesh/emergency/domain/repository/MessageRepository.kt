/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.domain.repository

import com.mesh.emergency.core.common.result.Result
import com.mesh.emergency.core.model.BaseModel
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface defining local and mesh database query pathways for messages.
 */
interface MessageRepository {
    /** Returns flow query list matching a single recipient/contact conversation. */
    fun getMessages(contactId: String): Flow<Result<List<MessageDomainModel>>>

    /** Enqueues and schedules a message packet transmission. */
    suspend fun sendMessage(recipientId: String, content: String): Result<Unit>

    /** Erases a message packet from local database. */
    suspend fun deleteMessage(messageId: String): Result<Unit>
}

/**
 * Domain model representing a communication packet.
 */
data class MessageDomainModel(
    override val id: String,
    val senderId: String,
    val recipientId: String,
    val content: String,
    val timestamp: Long,
    val status: MessageStatus
) : BaseModel

/**
 * Enumeration mapping message packet lifecycle states.
 */
enum class MessageStatus {
    SENDING,
    SENT,
    DELIVERED,
    FAILED
}
