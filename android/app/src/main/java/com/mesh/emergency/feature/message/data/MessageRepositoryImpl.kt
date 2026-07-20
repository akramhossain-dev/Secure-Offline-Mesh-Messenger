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

/**
 * Room-backed implementation of [MessageRepository] with lightweight BLE message transmission.
 *
 * Message send path:
 *   sendMessage() → builds MSG:{json} BLE packet → CommunicationManager.sendMessage()
 *
 * Message receive path (handled by BluetoothTransportImpl.handleInboundMessage()):
 *   BLE RX characteristic → MSG: prefix detection → Room insert → UI updates via Flow
 *
 * The previous heavy Packet/PacketSerializer/encryption pipeline is replaced with a
 * simpler format that is decoded symmetrically on the receiving device.
 */
@Singleton
class MessageRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val communicationManager: CommunicationManager,
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

    /**
     * Sends a chat message to the peer via BLE using the lightweight MSG: packet format.
     *
     * Packet: `MSG:{"id":"...","from":"senderId","to":"peerId","text":"...","ts":ms}`
     *
     * The [conversationId] of the [Message] is used as the recipient identifier because
     * in this app's design conversationId == peer's entityId (set when pairing).
     */
    override suspend fun sendMessage(message: Message) {
        // Resolve the actual local user identity (prefer profile userId over device fingerprint)
        val currentUser = localDataSource.getCurrentUser().firstOrNull()
        val senderId    = currentUser?.entityId?.takeIf { it.isNotBlank() }
            ?: deviceFingerprintProvider.getDeviceFingerprint()

        // Resolve recipient display name for the local conversation record
        val peerId       = message.conversationId  // conversationId == peer entityId
        val peerUser     = userDao.getUserById(peerId)
        val peerName     = peerUser?.nickname?.takeIf { it.isNotBlank() }
            ?: peerUser?.username?.takeIf { it.isNotBlank() }
            ?: peerId

        // Build lightweight BLE message packet
        val msgJson = JSONObject().apply {
            put("id",   message.id)
            put("from", senderId)
            put("to",   peerId)
            put("text", message.content)
            put("ts",   message.timestamp)
        }.toString()

        val blePayload = "MSG:$msgJson".toByteArray(Charsets.UTF_8)
        Timber.d("MSG_FLOW: Sending message to '$peerName' (${blePayload.size} bytes) id=${message.id}")

        // Attempt BLE delivery
        val result = try {
            communicationManager.sendMessage(blePayload)
        } catch (e: Exception) {
            Timber.e(e, "MSG_FLOW: BLE send exception for message ${message.id}")
            Result.Error(e)
        }

        val finalStatus = when (result) {
            is Result.Success -> {
                Timber.d("MSG_FLOW: Message delivered via BLE — id=${message.id}")
                DbDeliveryStatus.DELIVERED
            }
            else -> {
                Timber.w("MSG_FLOW: BLE send failed — message queued — id=${message.id}")
                DbDeliveryStatus.QUEUED
            }
        }

        // Persist outbound message to Room (sender side)
        val entity = message.toEntity().copy(
            senderId       = senderId,
            recipientId    = peerId,
            conversationId = peerId,   // conversation keyed by peer's userId (same as receive side)
            deliveryStatus = finalStatus
        )
        messageDao.insertMessage(entity)

        // Upsert conversation row so it appears in MessageListScreen
        conversationDao.insertConversation(
            ConversationEntity(
                entityId      = peerId,
                title         = peerName,
                lastMessageId = message.id,
                unreadCount   = 0,
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
}
