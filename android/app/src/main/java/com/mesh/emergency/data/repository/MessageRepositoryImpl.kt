/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.repository

import com.mesh.emergency.core.common.result.Result
import com.mesh.emergency.core.identity.DeviceFingerprintProvider
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
    private val localDataSource: LocalDataSource,
    private val deviceFingerprintProvider: DeviceFingerprintProvider
) : MessageRepository {

    override fun getMessages(contactId: String): Flow<Result<List<MessageDomainModel>>> {
        return localDataSource.getMessagesForConversation(contactId).map { entities ->
            Result.Success(entities.map { it.toDomain() })
        }
    }

    override suspend fun sendMessage(recipientId: String, content: String): Result<Unit> {
        return try {
            val now = System.currentTimeMillis()
            val message = MessageEntity(
                entityId = UUID.randomUUID().toString(),
                conversationId = recipientId,
                senderId = deviceFingerprintProvider.getDeviceFingerprint(),
                recipientId = recipientId,
                content = content,
                timestamp = now,
                deliveryStatus = DbDeliveryStatus.PENDING,
                type = DbMessageType.TEXT,
                priority = DbMessagePriority.MEDIUM,
                expiryTime = now + 86400000L, // 1 day expiry by default
                retryCount = 0
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
        DbDeliveryStatus.PENDING   -> MessageStatus.SENDING
        DbDeliveryStatus.QUEUED    -> MessageStatus.SENDING
        DbDeliveryStatus.SENDING   -> MessageStatus.SENDING
        DbDeliveryStatus.SENT      -> MessageStatus.SENT
        DbDeliveryStatus.DELIVERED -> MessageStatus.DELIVERED
        DbDeliveryStatus.FAILED    -> MessageStatus.FAILED
        DbDeliveryStatus.EXPIRED   -> MessageStatus.FAILED
    }
)
