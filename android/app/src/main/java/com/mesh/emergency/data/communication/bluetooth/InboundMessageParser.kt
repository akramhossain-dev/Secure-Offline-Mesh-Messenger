/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.communication.bluetooth

import com.mesh.emergency.core.notification.InboundNotificationCoordinator
import com.mesh.emergency.core.notification.MessageNotifier
import com.mesh.emergency.data.local.LocalDataSource
import com.mesh.emergency.data.local.entity.ConversationEntity
import com.mesh.emergency.data.local.entity.DbDeliveryStatus
import com.mesh.emergency.data.local.entity.DbMessagePriority
import com.mesh.emergency.data.local.entity.DbMessageType
import com.mesh.emergency.data.local.entity.DeviceEntity
import com.mesh.emergency.data.local.entity.GlobalMessageEntity
import com.mesh.emergency.data.local.entity.MessageEntity
import com.mesh.emergency.data.local.entity.UserEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Encapsulates all inbound message envelope parsing, Room database persistence,
 * packet deduplication, and notification coordination for Bluetooth mesh transport.
 */
@Singleton
class InboundMessageParser @Inject constructor(
    private val localDataSource: LocalDataSource,
    private val notificationCoordinator: InboundNotificationCoordinator,
    private val messageNotifier: MessageNotifier
) {
    /** Concurrent deduplication set preventing infinite mesh re-broadcast loops. */
    private val seenPacketIds = Collections.synchronizedSet(HashSet<String>())

    /**
     * Parses raw incoming string data from BLE characteristic read/notify.
     * Routes packet to appropriate handler based on prefix/content.
     */
    suspend fun parseAndProcessInbound(
        rawPayload: String,
        senderDeviceAddress: String?,
        scope: CoroutineScope,
        onRebroadcast: (String) -> Unit,
        onSendDirectPayload: (ByteArray) -> Unit,
        onUnparsedPayload: (String) -> Unit
    ) {
        when {
            rawPayload.startsWith("GCHAT:") -> {
                handleInboundGlobalMessage(
                    jsonStr = rawPayload.removePrefix("GCHAT:"),
                    onRebroadcast = onRebroadcast
                )
            }
            rawPayload.startsWith("MSG:") -> {
                handleInboundMessage(
                    jsonStr = rawPayload.removePrefix("MSG:"),
                    onRebroadcast = onRebroadcast,
                    onSendDirectPayload = onSendDirectPayload
                )
            }
            rawPayload.startsWith("IDENTITY:") -> {
                Timber.d("PAIR_FLOW: IDENTITY packet received via BLE")
                handlePeerIdentity(
                    jsonStr = rawPayload.removePrefix("IDENTITY:"),
                    senderBleAddress = senderDeviceAddress
                )
            }
            rawPayload.contains("\"type\":\"REVERSE_HANDSHAKE\"") -> {
                handleReverseHandshake(
                    jsonStr = rawPayload,
                    senderBleAddress = senderDeviceAddress,
                    onUnparsedPayload = onUnparsedPayload
                )
            }
            else -> {
                onUnparsedPayload(rawPayload)
            }
        }
    }

    private suspend fun handleInboundGlobalMessage(
        jsonStr: String,
        onRebroadcast: (String) -> Unit
    ) {
        try {
            Timber.d("GCHAT_FLOW: Inbound global message received — ${jsonStr.take(120)}")
            val json       = JSONObject(jsonStr)
            val type       = json.optString("type", "chat")
            val msgId      = json.getString("id")
            val senderId   = json.getString("from")
            val senderName = json.optString("name", "Unknown")
            val timestamp  = json.optLong("ts", System.currentTimeMillis())

            val actionKey = "gchat:${msgId}:${type}:${timestamp}"
            if (!seenPacketIds.add(actionKey)) {
                return
            }

            val existing = localDataSource.getGlobalMessageById(msgId)

            when (type) {
                "chat" -> {
                    if (existing == null) {
                        val text = json.getString("text")
                        val replyToId = json.optString("replyToId").takeIf { it.isNotEmpty() }
                        val replyToName = json.optString("replyToName").takeIf { it.isNotEmpty() }
                        val replyToText = json.optString("replyToText").takeIf { it.isNotEmpty() }
                        val entity = GlobalMessageEntity(
                            messageId      = msgId,
                            senderId       = senderId,
                            senderName     = senderName,
                            content        = text,
                            timestamp      = timestamp,
                            createdAt      = timestamp,
                            updatedAt      = timestamp,
                            edited         = false,
                            deleted        = false,
                            deliveryStatus = "DELIVERED",
                            readStatus     = "READ",
                            syncState      = "SYNCED",
                            editHistory    = emptyList(),
                            replyToMessageId = replyToId,
                            replyToSenderName = replyToName,
                            replyToContent = replyToText
                        )
                        localDataSource.insertGlobalMessage(entity)

                        messageNotifier.notifyGlobalMessage(msgId, senderId, senderName, text)
                        notificationCoordinator.notifyInboundMessage(msgId, senderName, text, isGlobal = true)
                        onRebroadcast("GCHAT:$jsonStr")
                    } else if (timestamp > existing.updatedAt) {
                        val updated = existing.copy(
                            content = json.getString("text"),
                            updatedAt = timestamp
                        )
                        localDataSource.updateGlobalMessage(updated)
                        onRebroadcast("GCHAT:$jsonStr")
                    }
                }
                "edit" -> {
                    val text = json.getString("text")
                    if (existing == null) {
                        val entity = GlobalMessageEntity(
                            messageId      = msgId,
                            senderId       = senderId,
                            senderName     = senderName,
                            content        = text,
                            timestamp      = timestamp,
                            createdAt      = timestamp,
                            updatedAt      = timestamp,
                            edited         = true,
                            deleted        = false,
                            deliveryStatus = "DELIVERED",
                            readStatus     = "READ",
                            syncState      = "SYNCED",
                            editHistory    = emptyList()
                        )
                        localDataSource.insertGlobalMessage(entity)
                        onRebroadcast("GCHAT:$jsonStr")
                    } else if (timestamp > existing.updatedAt) {
                        val history = existing.editHistory.toMutableList()
                        if (!history.contains(existing.content)) {
                            history.add(existing.content)
                        }
                        val updated = existing.copy(
                            content = text,
                            edited = true,
                            updatedAt = timestamp,
                            editHistory = history
                        )
                        localDataSource.updateGlobalMessage(updated)
                        onRebroadcast("GCHAT:$jsonStr")
                    }
                }
                "delete" -> {
                    if (existing == null) {
                        val entity = GlobalMessageEntity(
                            messageId      = msgId,
                            senderId       = senderId,
                            senderName     = senderName,
                            content        = "",
                            timestamp      = timestamp,
                            createdAt      = timestamp,
                            updatedAt      = timestamp,
                            edited         = false,
                            deleted        = true,
                            deliveryStatus = "DELIVERED",
                            readStatus     = "READ",
                            syncState      = "SYNCED",
                            editHistory    = emptyList()
                        )
                        localDataSource.insertGlobalMessage(entity)
                        onRebroadcast("GCHAT:$jsonStr")
                    } else if (timestamp > existing.updatedAt) {
                        val updated = existing.copy(
                            deleted = true,
                            updatedAt = timestamp
                        )
                        localDataSource.updateGlobalMessage(updated)
                        onRebroadcast("GCHAT:$jsonStr")
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "GCHAT_FLOW: Failed to parse inbound GCHAT: packet — $jsonStr")
        }
    }

    private suspend fun handleInboundMessage(
        jsonStr: String,
        onRebroadcast: (String) -> Unit,
        onSendDirectPayload: (ByteArray) -> Unit
    ) {
        try {
            Timber.d("MSG_FLOW: Inbound message received — ${jsonStr.take(120)}")
            val json       = JSONObject(jsonStr)
            val type       = json.optString("type", "chat")
            val msgId      = json.getString("id")
            val senderId   = json.getString("from")
            val receiverId = json.optString("to", "")
            val timestamp  = json.optLong("ts", System.currentTimeMillis())

            val currentUser = localDataSource.getCurrentUser().firstOrNull()
            val localUserId = currentUser?.entityId ?: "self"

            if (receiverId == localUserId) {
                val actionKey = "msg:${msgId}:${type}:${timestamp}"
                if (!seenPacketIds.add(actionKey)) {
                    return
                }

                val senderUser = localDataSource.getUserById(senderId)
                val senderName = senderUser?.nickname?.takeIf { it.isNotBlank() }
                    ?: senderUser?.username?.takeIf { it.isNotBlank() }
                    ?: "Contact-${senderId.take(6)}"

                val existing = localDataSource.getMessageById(msgId)

                when (type) {
                    "chat" -> {
                        if (existing == null) {
                            val text = json.getString("text")
                            val replyToId = json.optString("replyToId").takeIf { it.isNotEmpty() }
                            val replyToName = json.optString("replyToName").takeIf { it.isNotEmpty() }
                            val replyToText = json.optString("replyToText").takeIf { it.isNotEmpty() }
                            val messageEntity = MessageEntity(
                                entityId       = msgId,
                                messageId      = msgId,
                                conversationId = senderId,
                                senderId       = senderId,
                                senderName     = senderName,
                                recipientId    = receiverId,
                                content        = text,
                                timestamp      = timestamp,
                                createdAt      = timestamp,
                                updatedAt      = timestamp,
                                edited         = false,
                                deleted        = false,
                                deliveryStatus = DbDeliveryStatus.DELIVERED,
                                readStatus     = "UNREAD",
                                syncState      = "SYNCED",
                                editHistory    = emptyList(),
                                type           = DbMessageType.TEXT,
                                priority       = DbMessagePriority.MEDIUM,
                                expiryTime     = timestamp + 86_400_000L,
                                retryCount     = 0,
                                replyToMessageId = replyToId,
                                replyToSenderName = replyToName,
                                replyToContent = replyToText
                            )
                            val conversationEntity = ConversationEntity(
                                entityId      = senderId,
                                title         = senderName,
                                lastMessageId = msgId,
                                unreadCount   = 1,
                                updatedAt     = timestamp
                            )
                            localDataSource.insertMessage(messageEntity)
                            localDataSource.insertConversation(conversationEntity)

                            messageNotifier.notifyPrivateMessage(msgId, senderId, senderName, text)
                            notificationCoordinator.notifyInboundMessage(msgId, senderName, text, isGlobal = false, sourceId = senderId)

                            // Acknowledge delivery immediately
                            sendDeliveryReceipt(msgId, localUserId, senderId, onSendDirectPayload)
                        }
                    }
                    "edit" -> {
                        val text = json.getString("text")
                        if (existing != null && timestamp > existing.updatedAt) {
                            val history = existing.editHistory.toMutableList()
                            if (!history.contains(existing.content)) {
                                history.add(existing.content)
                            }
                            val updated = existing.copy(
                                content = text,
                                edited = true,
                                updatedAt = timestamp,
                                editHistory = history
                            )
                            localDataSource.insertMessage(updated)
                        }
                    }
                    "delete" -> {
                        if (existing != null && timestamp > existing.updatedAt) {
                            val updated = existing.copy(
                                deleted = true,
                                updatedAt = timestamp
                            )
                            localDataSource.insertMessage(updated)
                        }
                    }
                    "delivery_receipt" -> {
                        if (existing != null && existing.deliveryStatus != DbDeliveryStatus.DELIVERED) {
                            val updated = existing.copy(
                                deliveryStatus = DbDeliveryStatus.DELIVERED
                            )
                            localDataSource.insertMessage(updated)
                        }
                    }
                    "read_receipt" -> {
                        if (existing != null) {
                            val updated = existing.copy(
                                deliveryStatus = DbDeliveryStatus.DELIVERED,
                                readStatus = "READ"
                            )
                            localDataSource.insertMessage(updated)
                        }
                    }
                }
            } else {
                // Forward packet to peers (Mesh Routing)
                val actionKey = "msg:${msgId}:${type}:${timestamp}"
                if (seenPacketIds.add(actionKey)) {
                    Timber.d("MSG_FLOW: Message not for us. Forwarding to mesh — id=$msgId to=$receiverId")
                    onRebroadcast("MSG:$jsonStr")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "MSG_FLOW: Failed to parse inbound MSG: packet — $jsonStr")
        }
    }

    private suspend fun handlePeerIdentity(jsonStr: String, senderBleAddress: String?) {
        try {
            val json = JSONObject(jsonStr)
            val uid  = json.getString("uid")
            val name = json.optString("un", "").ifBlank { "Contact-${uid.take(6)}" }
            val ble  = senderBleAddress?.takeIf { it != "02:00:00:00:00:00" }
                ?: json.optString("ble", "")

            val userEntity = UserEntity(
                entityId = uid,
                username = name,
                profileImageRef = null,
                languagePreference = "en",
                createdTime = System.currentTimeMillis(),
                updatedTime = System.currentTimeMillis(),
                status = "ACTIVE",
                isCurrentUser = false,
                lastSeen = System.currentTimeMillis(),
                trustedStatus = true,
                nickname = name
            )
            val deviceEntity = DeviceEntity(
                entityId = uid,
                name = name,
                rssi = -55,
                lastSeen = System.currentTimeMillis(),
                deviceType = "SMARTPHONE",
                platformInfo = "ANDROID",
                createdTime = System.currentTimeMillis(),
                lastActiveTime = System.currentTimeMillis(),
                trustStatus = com.mesh.emergency.data.local.entity.DbTrustStatus.TRUSTED,
                nickname = name,
                bleAddress = ble
            )
            localDataSource.insertUser(userEntity)
            localDataSource.insertDevice(deviceEntity)
            Timber.d("PAIR_FLOW: Peer identity saved — uid=$uid name='$name' ble=$ble")
        } catch (e: Exception) {
            Timber.e(e, "PAIR_FLOW: Failed to parse peer identity packet")
        }
    }

    private suspend fun handleReverseHandshake(
        jsonStr: String,
        senderBleAddress: String?,
        onUnparsedPayload: (String) -> Unit
    ) {
        try {
            val json = JSONObject(jsonStr)
            val remoteUserId    = json.getString("uid")
            val remoteUsername  = json.optString("un", "").ifBlank { "Contact-${remoteUserId.take(6)}" }
            val remotePublicKey = json.optString("pub", "")
            val realBleAddress  = senderBleAddress?.takeIf { it != "02:00:00:00:00:00" }
                ?: json.optString("ble", "")
            val remoteDeviceType = json.optString("dt", "SMARTPHONE")

            Timber.d("PAIR_FLOW: Pair request received — uid=$remoteUserId un='$remoteUsername' ble=$realBleAddress")

            val userEntity = UserEntity(
                entityId = remoteUserId,
                username = remoteUsername,
                profileImageRef = null,
                languagePreference = "en",
                createdTime = System.currentTimeMillis(),
                updatedTime = System.currentTimeMillis(),
                status = "ACTIVE",
                isCurrentUser = false,
                lastSeen = System.currentTimeMillis(),
                trustedStatus = true,
                nickname = remoteUsername,
                publicKey = remotePublicKey
            )
            val deviceEntity = DeviceEntity(
                entityId = remoteUserId,
                name = remoteUsername,
                rssi = -55,
                lastSeen = System.currentTimeMillis(),
                deviceType = remoteDeviceType,
                platformInfo = "ANDROID",
                createdTime = System.currentTimeMillis(),
                lastActiveTime = System.currentTimeMillis(),
                trustStatus = com.mesh.emergency.data.local.entity.DbTrustStatus.TRUSTED,
                nickname = remoteUsername,
                bleAddress = realBleAddress
            )
            localDataSource.insertUser(userEntity)
            localDataSource.insertDevice(deviceEntity)
            Timber.d("PAIR_FLOW: Pair accepted — bidirectional pairing complete for $remoteUserId")
        } catch (e: Exception) {
            Timber.e(e, "PAIR_FLOW: Failed to parse reverse handshake")
            onUnparsedPayload(jsonStr)
        }
    }

    private fun sendDeliveryReceipt(
        messageId: String,
        senderId: String,
        recipientId: String,
        onSendDirectPayload: (ByteArray) -> Unit
    ) {
        try {
            val json = JSONObject().apply {
                put("type", "delivery_receipt")
                put("id",   messageId)
                put("from", senderId)
                put("to",   recipientId)
                put("ts",   System.currentTimeMillis())
            }
            onSendDirectPayload("MSG:$json".toByteArray(Charsets.UTF_8))
        } catch (e: Exception) {
            Timber.e(e, "RECEIPT: Failed to send delivery receipt for $messageId")
        }
    }
}
