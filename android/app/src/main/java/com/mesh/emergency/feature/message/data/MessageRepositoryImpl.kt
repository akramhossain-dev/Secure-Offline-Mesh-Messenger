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
import com.mesh.emergency.core.communication.CommunicationState
import com.mesh.emergency.core.communication.DeliveryResult
import com.mesh.emergency.core.identity.DeviceFingerprintProvider
import com.mesh.emergency.core.protocol.Packet
import com.mesh.emergency.core.protocol.PacketHeader
import com.mesh.emergency.core.protocol.PacketSerializer
import com.mesh.emergency.core.protocol.PacketValidator
import com.mesh.emergency.core.protocol.PacketValidationResult
import com.mesh.emergency.core.security.CryptographyEngine
import com.mesh.emergency.core.security.KeyManager
import com.mesh.emergency.feature.message.domain.ConversationSummary
import com.mesh.emergency.feature.message.domain.Message
import com.mesh.emergency.feature.message.domain.MessageRepository
import com.mesh.emergency.feature.message.domain.toDomain
import com.mesh.emergency.feature.message.domain.toEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room-backed implementation of [MessageRepository] and real-time communication bridge.
 */
@Singleton
class MessageRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val communicationManager: CommunicationManager,
    private val keyManager: KeyManager,
    private val cryptographyEngine: CryptographyEngine,
    private val packetValidator: PacketValidator,
    private val deviceFingerprintProvider: DeviceFingerprintProvider
) : MessageRepository {

    private val messageDao = database.messageDao()
    private val conversationDao = database.conversationDao()
    private val userDao = database.userDao()

    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        observeInboundPackets()
    }

    private fun observeInboundPackets() {
        scope.launch {
            communicationManager.receiveMessages().collect { bytes ->
                try {
                    val packet = PacketSerializer.deserializeFromJson(String(bytes, Charsets.UTF_8))
                    if (packetValidator.validate(packet) == PacketValidationResult.VALID) {
                        val senderId = packet.header.senderId
                        val senderUser = userDao.getUserById(senderId)
                        val decryptedContent = if (senderUser?.publicKey != null) {
                            val peerPubKeyBytes = senderUser.publicKey.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
                            val sharedSecret = keyManager.deriveSharedSecret(peerPubKeyBytes)
                            val decryptResult = decryptPayload(packet.encryptedPayload, sharedSecret, cryptographyEngine)
                            if (decryptResult is Result.Success) {
                                String(decryptResult.data, Charsets.UTF_8)
                            } else {
                                null
                            }
                        } else {
                            String(packet.encryptedPayload, Charsets.UTF_8)
                        }

                        if (decryptedContent != null) {
                            val messageEntity = MessageEntity(
                                entityId = packet.header.packetId,
                                conversationId = senderId,
                                senderId = senderId,
                                recipientId = packet.header.receiverId,
                                content = decryptedContent,
                                timestamp = packet.header.timestamp,
                                deliveryStatus = DbDeliveryStatus.DELIVERED,
                                type = packet.header.messageType,
                                priority = packet.header.priority,
                                expiryTime = packet.header.ttl,
                                retryCount = 0
                            )
                            messageDao.insertMessage(messageEntity)

                            val conversation = ConversationEntity(
                                entityId = senderId,
                                title = senderUser?.username ?: "Contact-${senderId.take(6)}",
                                lastMessageId = packet.header.packetId,
                                unreadCount = 0,
                                updatedAt = packet.header.timestamp
                            )
                            conversationDao.insertConversation(conversation)
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "MessageRepository: Inbound packet processing failed")
                }
            }
        }
    }

    override fun getConversations(): Flow<List<ConversationSummary>> =
        conversationDao.getConversationsWithLastMessage().map { conversations ->
            conversations.map { entity ->
                ConversationSummary(
                    id           = entity.entityId,
                    title        = entity.title,
                    lastMessagePreview = entity.lastMessageContent ?: "",
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
        val senderId = deviceFingerprintProvider.getDeviceFingerprint()
        val recipientUser = userDao.getUserById(message.recipientId)

        val plainBytes = message.content.toByteArray(Charsets.UTF_8)
        val encryptedBytes = if (recipientUser?.publicKey != null) {
            val peerPubKeyBytes = recipientUser.publicKey.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
            val sharedSecret = keyManager.deriveSharedSecret(peerPubKeyBytes)
            val encryptResult = encryptPayload(plainBytes, sharedSecret, cryptographyEngine)
            if (encryptResult is Result.Success) {
                encryptResult.data
            } else {
                plainBytes
            }
        } else {
            plainBytes
        }

        val header = PacketHeader(
            version = 1,
            packetId = message.id,
            senderId = senderId,
            receiverId = message.recipientId,
            messageType = DbMessageType.valueOf(message.type.name),
            priority = DbMessagePriority.valueOf(message.priority.name),
            ttl = message.expiryTime,
            hopCount = 0,
            timestamp = message.timestamp
        )
        val checksum = PacketSerializer.calculateChecksum(header, encryptedBytes)
        val packet = Packet(header, encryptedBytes, null, checksum)
        val serializedJson = PacketSerializer.serializeToJson(packet)

        val activeTransport = communicationManager.activeTransport.value
        val result = if (activeTransport != null && communicationManager.getAvailableTransports().isNotEmpty()) {
            communicationManager.sendMessage(serializedJson.toByteArray(Charsets.UTF_8))
        } else {
            Result.Error(Exception("No active transport channels available"))
        }

        val finalStatus = when (result) {
            is Result.Success -> DbDeliveryStatus.DELIVERED
            else -> DbDeliveryStatus.QUEUED
        }

        val entity = message.toEntity().copy(deliveryStatus = finalStatus, senderId = senderId)
        messageDao.insertMessage(entity)

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

    private fun encryptPayload(
        plainText: ByteArray,
        secretKey: ByteArray,
        cryptoEngine: CryptographyEngine
    ): Result<ByteArray> {
        val encryptResult = cryptoEngine.encrypt(plainText, secretKey)
        return when (encryptResult) {
            is Result.Success -> {
                val iv = encryptResult.data.iv
                val cipherText = encryptResult.data.cipherText
                Result.Success(iv + cipherText)
            }
            is Result.Error -> Result.Error(encryptResult.exception)
            else -> Result.Error(Exception("Encryption failed"))
        }
    }

    private fun decryptPayload(
        ivAndCipherText: ByteArray,
        secretKey: ByteArray,
        cryptoEngine: CryptographyEngine
    ): Result<ByteArray> {
        if (ivAndCipherText.size < 12) return Result.Error(Exception("Invalid payload size"))
        val iv = ivAndCipherText.copyOfRange(0, 12)
        val cipherText = ivAndCipherText.copyOfRange(12, ivAndCipherText.size)
        return cryptoEngine.decrypt(cipherText, secretKey, iv)
    }
}
