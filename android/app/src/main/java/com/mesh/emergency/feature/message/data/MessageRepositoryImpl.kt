/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.feature.message.data

import com.mesh.emergency.data.local.dao.ConversationDao
import com.mesh.emergency.data.local.dao.MessageDao
import com.mesh.emergency.data.local.entity.ConversationEntity
import com.mesh.emergency.feature.message.domain.ConversationSummary
import com.mesh.emergency.feature.message.domain.Message
import com.mesh.emergency.feature.message.domain.MessageRepository
import com.mesh.emergency.feature.message.domain.toDomain
import com.mesh.emergency.feature.message.domain.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room-backed implementation of [MessageRepository].
 * Joins [MessageDao] and [ConversationDao] to build conversation summaries.
 */
@Singleton
class MessageRepositoryImpl @Inject constructor(
    private val messageDao: MessageDao,
    private val conversationDao: ConversationDao
) : MessageRepository {

    override fun getConversations(): Flow<List<ConversationSummary>> =
        conversationDao.getConversations().map { conversations ->
            conversations.map { entity ->
                ConversationSummary(
                    id           = entity.entityId,
                    title        = entity.title,
                    lastMessagePreview = "Loading…",
                    unreadCount  = entity.unreadCount,
                    updatedAt    = entity.updatedAt
                )
            }
        }

    override fun getMessagesForConversation(conversationId: String): Flow<List<Message>> =
        messageDao.getMessagesForConversation(conversationId).map { list ->
            list.map { it.toDomain() }
        }

    override suspend fun sendMessage(message: Message) {
        messageDao.insertMessage(message.toEntity())
        // Update conversation summary
        conversationDao.insertConversation(
            ConversationEntity(
                entityId      = message.conversationId,
                title         = message.recipientId,
                lastMessageId = message.id,
                unreadCount   = 0,
                updatedAt     = message.timestamp
            )
        )
    }

    override suspend fun deleteMessage(messageId: String) {
        val entity = messageDao.getMessageById(messageId) ?: return
        messageDao.deleteMessage(entity)
    }

    override suspend fun getMessageById(messageId: String): Message? =
        messageDao.getMessageById(messageId)?.toDomain()
}
