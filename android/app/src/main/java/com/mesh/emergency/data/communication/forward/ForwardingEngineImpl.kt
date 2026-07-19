/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.communication.forward

import com.mesh.emergency.core.common.result.Result
import com.mesh.emergency.core.communication.CommunicationManager
import com.mesh.emergency.core.communication.forward.ForwardingEngine
import com.mesh.emergency.core.communication.queue.MessageQueueManager
import com.mesh.emergency.core.protocol.Packet
import com.mesh.emergency.core.protocol.PacketHeader
import com.mesh.emergency.core.protocol.PacketSerializer
import com.mesh.emergency.core.security.CryptographyEngine
import com.mesh.emergency.core.security.KeyManager
import com.mesh.emergency.data.local.LocalDataSource
import com.mesh.emergency.data.local.entity.DbDeliveryStatus
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [ForwardingEngine] managing backoff retry waits and TTL wipes.
 */
@Singleton
class ForwardingEngineImpl @Inject constructor(
    private val queueManager: MessageQueueManager,
    private val communicationManager: CommunicationManager,
    private val localDataSource: LocalDataSource,
    private val keyManager: KeyManager,
    private val cryptographyEngine: CryptographyEngine
) : ForwardingEngine {

    private val MAX_RETRIES_LIMIT = 5

    override suspend fun processQueue() {
        val pendingMessages = queueManager.getPendingQueue().first()
        val now = System.currentTimeMillis()

        for (message in pendingMessages) {
            // 1. Check expiration (TTL Sweep)
            if (now > message.expiryTime) {
                queueManager.updateStatus(message.entityId, DbDeliveryStatus.EXPIRED)
                continue
            }

            // 2. Check retry backoff wait criteria
            val elapsed = now - message.timestamp
            val requiredWait = when (message.retryCount) {
                0    -> 0L
                1    -> 5000L      // 5 seconds
                2    -> 30000L     // 30 seconds
                else -> 300000L    // 5 minutes
            }

            if (elapsed < requiredWait) {
                continue // Skip, backoff wait interval has not elapsed yet
            }

            // 3. Select active connected transceiver and write
            val activeTransport = communicationManager.activeTransport.value
            if (activeTransport == null || communicationManager.getAvailableTransports().isEmpty()) {
                // If offline and max retries exceeded, mark failed
                if (message.retryCount >= MAX_RETRIES_LIMIT) {
                    queueManager.updateStatus(message.entityId, DbDeliveryStatus.FAILED)
                }
                continue
            }

            queueManager.updateStatus(message.entityId, DbDeliveryStatus.SENDING)

            // Retrieve recipient public key for encryption
            val recipientId = message.recipientId
            val recipientUser = localDataSource.getUserById(recipientId)
            val plainBytes = message.content.toByteArray(Charsets.UTF_8)
            val encryptedBytes = if (recipientUser?.publicKey != null) {
                val peerPubKeyBytes = recipientUser.publicKey.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
                val sharedSecret = keyManager.deriveSharedSecret(peerPubKeyBytes)
                val encryptResult = encryptPayload(plainBytes, sharedSecret, cryptographyEngine)
                if (encryptResult is Result.Success) encryptResult.data else plainBytes
            } else {
                plainBytes
            }

            val header = PacketHeader(
                version = 1,
                packetId = message.entityId,
                senderId = message.senderId,
                receiverId = message.recipientId,
                messageType = message.type,
                priority = message.priority,
                ttl = message.expiryTime,
                hopCount = 0,
                timestamp = message.timestamp
            )
            val checksum = PacketSerializer.calculateChecksum(header, encryptedBytes)
            val packet = Packet(header, encryptedBytes, null, checksum)
            val serializedJson = PacketSerializer.serializeToJson(packet)

            val result = communicationManager.sendMessage(serializedJson.toByteArray(Charsets.UTF_8))
            when (result) {
                is Result.Success -> {
                    queueManager.updateStatus(message.entityId, DbDeliveryStatus.DELIVERED)
                }
                is Result.Error -> {
                    queueManager.incrementRetry(message.entityId)
                    if (message.retryCount + 1 >= MAX_RETRIES_LIMIT) {
                        queueManager.updateStatus(message.entityId, DbDeliveryStatus.FAILED)
                    } else {
                        queueManager.updateStatus(message.entityId, DbDeliveryStatus.QUEUED)
                    }
                }
                is Result.Loading -> {
                    // Do nothing, await outcome
                }
            }
        }
    }

    override suspend fun cleanExpiredMessages() {
        val now = System.currentTimeMillis()
        // Clean expired pending/queued messages in database
        val pendingMessages = queueManager.getPendingQueue().first()
        for (message in pendingMessages) {
            if (now > message.expiryTime) {
                queueManager.updateStatus(message.entityId, DbDeliveryStatus.EXPIRED)
            }
        }
    }

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
}
