/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.repository

import com.mesh.emergency.core.common.result.Result
import com.mesh.emergency.data.local.LocalDataSource
import com.mesh.emergency.data.local.entity.DbDeliveryStatus
import com.mesh.emergency.data.local.entity.DbMessagePriority
import com.mesh.emergency.data.local.entity.DbMessageType
import com.mesh.emergency.data.local.entity.MessageEntity
import com.mesh.emergency.domain.repository.MessageDomainModel
import com.mesh.emergency.domain.repository.MessageRepository
import com.mesh.emergency.domain.repository.MessageStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository regulating messaging logs query transactions using Room database.
 */
@Singleton
class MessageRepositoryImpl @Inject constructor(
    private val localDataSource: LocalDataSource
) : MessageRepository {

    override fun getMessages(contactId: String): Flow<Result<List<MessageDomainModel>>> {
        return localDataSource.getMessagesForConversation(contactId).map { entities ->
            Result.Success(entities.map { it.toDomain() })
        }
    }

    override suspend fun sendMessage(recipientId: String, content: String): Result<Unit> {
        return try {
            val message = MessageEntity(
                entityId = UUID.randomUUID().toString(),
                conversationId = recipientId,
                senderId = "local_user_id",
                recipientId = recipientId,
                content = content,
                timestamp = System.currentTimeMillis(),
                deliveryStatus = DbDeliveryStatus.PENDING,
                type = DbMessageType.TEXT,
                priority = DbMessagePriority.MEDIUM
            )
            localDataSource.insertMessage(message)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun deleteMessage(messageId: String): Result<Unit> {
        return try {
            val message = localDataSource.getMessageById(messageId)
            if (message != null) {
                localDataSource.deleteMessage(message)
                Result.Success(Unit)
            } else {
                Result.Error(Exception("Message not found"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}

private fun MessageEntity.toDomain(): MessageDomainModel = MessageDomainModel(
    id = entityId,
    senderId = senderId,
    recipientId = recipientId,
    content = content,
    timestamp = timestamp,
    status = when (deliveryStatus) {
        DbDeliveryStatus.PENDING -> MessageStatus.SENDING
        DbDeliveryStatus.SENT -> MessageStatus.SENT
        DbDeliveryStatus.DELIVERED -> MessageStatus.DELIVERED
        DbDeliveryStatus.FAILED -> MessageStatus.FAILED
    }
)
