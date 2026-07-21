/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.feature.message.data

import com.mesh.emergency.data.local.database.AppDatabase
import com.mesh.emergency.data.local.entity.ConversationEntity
import com.mesh.emergency.data.local.entity.DbDeliveryStatus
import com.mesh.emergency.data.local.entity.DbMessagePriority
import com.mesh.emergency.data.local.entity.DbMessageType
import com.mesh.emergency.data.local.entity.MessageEntity
import com.mesh.emergency.core.common.result.Result
import com.mesh.emergency.core.communication.CommunicationManager
import com.mesh.emergency.core.identity.DeviceFingerprintProvider
import com.mesh.emergency.data.local.LocalDataSource
import com.mesh.emergency.feature.message.domain.ConversationSummary
import com.mesh.emergency.feature.message.domain.Message
import com.mesh.emergency.feature.message.domain.MessageRepository
import com.mesh.emergency.feature.message.domain.toDomain
import com.mesh.emergency.feature.message.domain.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

import com.mesh.emergency.core.messaging.MessagingService

/**
 * Room-backed implementation of [MessageRepository] delegating to [MessagingService].
 */
@Singleton
class MessageRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val communicationManager: CommunicationManager,
    private val messagingService: MessagingService,
    private val deviceFingerprintProvider: DeviceFingerprintProvider,
    private val localDataSource: LocalDataSource
) : MessageRepository {

    private val messageDao      = database.messageDao()
    private val conversationDao = database.conversationDao()
    private val userDao         = database.userDao()

    // ── Queries ───────────────────────────────────────────────────────────────

    override fun getConversations(): Flow<List<ConversationSummary>> =
        conversationDao.getConversationsWithLastMessage().map { conversations ->
            conversations.map { entity ->
                ConversationSummary(
                    id                 = entity.entityId,
                    title              = entity.title,
                    lastMessagePreview = entity.lastMessageContent ?: "",
                    unreadCount        = entity.unreadCount,
                    updatedAt          = entity.updatedAt
                )
            }
        }

    override fun getMessagesForConversation(conversationId: String): Flow<List<Message>> =
        messageDao.getMessagesForConversation(conversationId).map { list ->
            list.map { it.toDomain() }
        }

    // ── Send ──────────────────────────────────────────────────────────────────

    override suspend fun sendMessage(message: Message) {
        // Resolve the actual local user identity
        val currentUser = localDataSource.getCurrentUser().firstOrNull()
        val senderId    = currentUser?.entityId?.takeIf { it.isNotBlank() }
            ?: deviceFingerprintProvider.getDeviceFingerprint()

        val senderName = currentUser?.nickname?.takeIf { it.isNotBlank() }
            ?: currentUser?.username?.takeIf { it.isNotBlank() }
            ?: "Me"

        // Resolve recipient display name for the local conversation record
        val peerId   = message.conversationId
        val peerUser = userDao.getUserById(peerId)
        val peerName = peerUser?.nickname?.takeIf { it.isNotBlank() }
            ?: peerUser?.username?.takeIf { it.isNotBlank() }
            ?: peerId

        // Delegate private message dispatch through central MessagingService
        val sent = messagingService.sendPrivateMessage(
            messageId   = message.id,
            senderId    = senderId,
            senderName  = senderName,
            recipientId = peerId,
            text        = message.content,
            replyToId   = message.replyToMessageId,
            replyToName = message.replyToSenderName,
            replyToText = message.replyToContent
        )

        val finalStatus = if (sent) DbDeliveryStatus.SENT else DbDeliveryStatus.QUEUED

        // Persist outbound message to Room
        val entity = message.toEntity().copy(
            senderId       = senderId,
            senderName     = senderName,
            recipientId    = peerId,
            conversationId = peerId,
            deliveryStatus = finalStatus,
            syncState      = if (sent) "SYNCED" else "PENDING"
        )
        messageDao.insertMessage(entity)

        val existingConv = conversationDao.getConversationById(peerId)
        conversationDao.insertConversation(
            ConversationEntity(
                entityId      = peerId,
                title         = peerName,
                lastMessageId = message.id,
                unreadCount   = existingConv?.unreadCount ?: 0,
                updatedAt     = message.timestamp
            )
        )
    }

    // ── Mutation ──────────────────────────────────────────────────────────────

    override suspend fun deleteMessage(messageId: String) {
        val entity = messageDao.getMessageById(messageId) ?: return
        messageDao.deleteMessage(entity)
    }

    override suspend fun getMessageById(messageId: String): Message? =
        messageDao.getMessageById(messageId)?.toDomain()

    override suspend fun updateMessage(message: Message) {
        messageDao.insertMessage(message.toEntity())
    }

    override suspend fun markMessageAsRead(messageId: String) {
        val existing = messageDao.getMessageById(messageId)
        if (existing != null) {
            val updated = existing.copy(readStatus = "READ")
            messageDao.insertMessage(updated)
            conversationDao.clearUnreadCount(existing.conversationId)
        }
    }

    override suspend fun clearUnreadCount(conversationId: String) {
        conversationDao.clearUnreadCount(conversationId)
    }
}
