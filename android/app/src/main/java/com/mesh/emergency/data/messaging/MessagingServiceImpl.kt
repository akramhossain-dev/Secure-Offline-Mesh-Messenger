/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.messaging

import com.mesh.emergency.core.communication.CommunicationManager
import com.mesh.emergency.core.common.result.Result
import com.mesh.emergency.core.communication.TransportType
import com.mesh.emergency.core.messaging.IncomingMessage
import com.mesh.emergency.core.messaging.MessagingService
import com.mesh.emergency.data.local.LocalDataSource
import com.mesh.emergency.data.local.entity.DbDeliveryStatus
import com.mesh.emergency.data.local.entity.DbMessagePriority
import com.mesh.emergency.data.local.entity.DbMessageType
import com.mesh.emergency.data.local.entity.GlobalMessageEntity
import com.mesh.emergency.data.local.entity.MessageEntity
import com.mesh.emergency.data.local.entity.ConversationEntity
import com.mesh.emergency.core.notification.AlertModel
import com.mesh.emergency.core.notification.AlertPriority
import com.mesh.emergency.core.notification.AlertType
import com.mesh.emergency.core.notification.NotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Transport-agnostic implementation of [MessagingService].
 *
 * This class owns all application-layer messaging logic:
 * - JSON serialization/deserialization of message envelopes
 * - Inbound packet routing (GCHAT, MSG, IDENTITY, REVERSE_HANDSHAKE)
 * - Database persistence for received messages
 * - Queue drain and history sync on peer connection
 * - System notification triggering for background alerts
 *
 * It delegates raw byte transport exclusively to [CommunicationManager],
 * which in turn delegates to whichever [com.mesh.emergency.core.communication.Transport]
 * is currently active (Bluetooth, LoRa, Wi-Fi Direct, etc.).
 */
@Singleton
class MessagingServiceImpl @Inject constructor(
    private val communicationManager: CommunicationManager,
    private val localDataSource: LocalDataSource,
    private val notificationManager: NotificationManager
) : MessagingService {

    private val scope = CoroutineScope(Dispatchers.IO)

    /** Emits decoded inbound messages to all subscribers. */
    private val _incomingMessages = MutableSharedFlow<IncomingMessage>(extraBufferCapacity = 64)

    /**
     * Deduplication set preventing infinite mesh forwarding loops.
     * Auto-evicts oldest entries when > 1000 items to prevent unbounded growth.
     */
    private val seenPacketIds: MutableSet<String> = Collections.synchronizedSet(
        object : java.util.LinkedHashSet<String>() {
            override fun add(element: String): Boolean {
                if (size > 1000) {
                    val it = iterator()
                    if (it.hasNext()) { it.next(); it.remove() }
                }
                return super.add(element)
            }
        }
    )

    init {
        // Subscribe to raw bytes from CommunicationManager and decode them
        observeRawInbound()
    }

    // ── Inbound Pipeline ──────────────────────────────────────────────────────

    /**
     * Subscribes to [CommunicationManager.receiveMessages] and routes each raw
     * packet through the appropriate decoder based on its protocol prefix.
     */
    private fun observeRawInbound() {
        scope.launch {
            communicationManager.receiveMessages().collect { bytes ->
                try {
                    val str = String(bytes, Charsets.UTF_8)
                    when {
                        str.startsWith("GCHAT:")      -> handleInboundGlobalMessage(str.removePrefix("GCHAT:"))
                        str.startsWith("MSG:")        -> handleInboundPrivateMessage(str.removePrefix("MSG:"))
                        str.startsWith("IDENTITY:")   -> handlePeerIdentityPacket(str.removePrefix("IDENTITY:"))
                        str.contains("\"type\":\"REVERSE_HANDSHAKE\"") -> handleReverseHandshake(str)
                        else -> { /* Not a recognized messaging protocol packet — ignore */ }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "MESSAGING: Failed to decode inbound packet")
                }
            }
        }
    }

    // ── Global Chat ───────────────────────────────────────────────────────────

    override suspend fun sendGlobalMessage(
        messageId: String,
        senderId: String,
        senderName: String,
        text: String,
        replyToId: String?,
        replyToName: String?,
        replyToText: String?
    ): Boolean {
        val json = JSONObject().apply {
            put("type", "chat")
            put("id",   messageId)
            put("from", senderId)
            put("name", senderName)
            put("text", text)
            put("ts",   System.currentTimeMillis())
            if (replyToId != null) {
                put("replyToId",   replyToId)
                put("replyToName", replyToName)
                put("replyToText", replyToText)
            }
        }
        val payload = "GCHAT:$json".toByteArray(Charsets.UTF_8)
        Timber.d("MESSAGING: Sending global message — id=$messageId name='$senderName' text='${text.take(40)}'")
        val result = communicationManager.sendMessage(payload)
        return result is com.mesh.emergency.core.common.result.Result.Success
    }

    override suspend fun sendGlobalMessageEdit(
        messageId: String,
        senderId: String,
        senderName: String,
        newText: String
    ): Boolean {
        val json = JSONObject().apply {
            put("type", "edit")
            put("id",   messageId)
            put("from", senderId)
            put("name", senderName)
            put("text", newText)
            put("ts",   System.currentTimeMillis())
        }
        val payload = "GCHAT:$json".toByteArray(Charsets.UTF_8)
        val result = communicationManager.sendMessage(payload)
        return result is com.mesh.emergency.core.common.result.Result.Success
    }

    override suspend fun sendGlobalMessageDelete(
        messageId: String,
        senderId: String,
        senderName: String
    ): Boolean {
        val json = JSONObject().apply {
            put("type", "delete")
            put("id",   messageId)
            put("from", senderId)
            put("name", senderName)
            put("ts",   System.currentTimeMillis())
        }
        val payload = "GCHAT:$json".toByteArray(Charsets.UTF_8)
        val result = communicationManager.sendMessage(payload)
        return result is com.mesh.emergency.core.common.result.Result.Success
    }

    // ── Private Messaging ─────────────────────────────────────────────────────

    override fun sendPrivateMessageEdit(
        messageId: String,
        senderId: String,
        recipientId: String,
        newText: String
    ) {
        val json = JSONObject().apply {
            put("type", "edit")
            put("id",   messageId)
            put("from", senderId)
            put("to",   recipientId)
            put("text", newText)
            put("ts",   System.currentTimeMillis())
        }
        sendRawPacket("MSG:$json")
    }

    override fun sendPrivateMessageDelete(
        messageId: String,
        senderId: String,
        recipientId: String
    ) {
        val json = JSONObject().apply {
            put("type", "delete")
            put("id",   messageId)
            put("from", senderId)
            put("to",   recipientId)
            put("ts",   System.currentTimeMillis())
        }
        sendRawPacket("MSG:$json")
    }

    // ── Receipts ──────────────────────────────────────────────────────────────

    override fun sendReadReceipt(messageId: String, senderId: String, recipientId: String) {
        val json = JSONObject().apply {
            put("type", "read_receipt")
            put("id",   messageId)
            put("from", senderId)
            put("to",   recipientId)
            put("ts",   System.currentTimeMillis())
        }
        sendRawPacket("MSG:$json")
    }

    override fun sendDeliveryReceipt(messageId: String, senderId: String, recipientId: String) {
        val json = JSONObject().apply {
            put("type", "delivery_receipt")
            put("id",   messageId)
            put("from", senderId)
            put("to",   recipientId)
            put("ts",   System.currentTimeMillis())
        }
        sendRawPacket("MSG:$json")
    }

    // ── Inbound Observation ───────────────────────────────────────────────────

    override fun observeIncomingMessages(): Flow<IncomingMessage> = _incomingMessages.asSharedFlow()

    // ── Queue Management ──────────────────────────────────────────────────────

    override suspend fun onPeerConnected(remoteUserId: String) {
        Timber.d("MESSAGING: Peer connected remoteUserId=$remoteUserId. Draining queue and syncing history...")
        drainQueue()

        // Synchronize recent global history (up to 50 items) to the newly connected peer
        val recentGlobal = localDataSource.getGlobalMessages().firstOrNull()?.take(50) ?: emptyList()
        for (msg in recentGlobal) {
            val json = JSONObject().apply {
                put("type", "chat")
                put("id",   msg.messageId)
                put("from", msg.senderId)
                put("name", msg.senderName)
                put("text", msg.content)
                put("ts",   msg.updatedAt)
            }.toString()
            val payload = "GCHAT:$json".toByteArray(Charsets.UTF_8)
            communicationManager.sendMessage(payload)
        }
    }

    override suspend fun drainQueue() {
        try {
            // Drain private messages queue
            val pendingPrivate = localDataSource.getPendingMessages().firstOrNull() ?: emptyList()
            for (msg in pendingPrivate) {
                val json = JSONObject().apply {
                    put("type", "chat")
                    put("id",   msg.entityId)
                    put("from", msg.senderId)
                    put("to",   msg.recipientId)
                    put("text", msg.content)
                    put("ts",   msg.timestamp)
                }.toString()
                val payload = "MSG:$json".toByteArray(Charsets.UTF_8)
                val result = communicationManager.sendMessage(payload)
                if (result is com.mesh.emergency.core.common.result.Result.Success) {
                    val updated = msg.copy(
                        deliveryStatus = DbDeliveryStatus.SENT,
                        syncState = "SYNCED"
                    )
                    localDataSource.insertMessage(updated)
                }
            }

            // Drain pending global messages
            val allGlobal = localDataSource.getGlobalMessages().firstOrNull() ?: emptyList()
            val pendingGlobal = allGlobal.filter { it.deliveryStatus == "QUEUED" || it.deliveryStatus == "SENDING" }
            for (msg in pendingGlobal) {
                val sent = sendGlobalMessage(
                    messageId  = msg.messageId,
                    senderId   = msg.senderId,
                    senderName = msg.senderName,
                    text       = msg.content
                )
                if (sent) {
                    localDataSource.updateGlobalMessageStatus(msg.messageId, "SENT")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "MESSAGING: Failed to drain queue")
        }
    }

    // ── Inbound Decoders ──────────────────────────────────────────────────────

    /** Returns the transport type name of the currently active transport, or "UNKNOWN". */
    private fun activeTransportTypeName(): String =
        communicationManager.activeTransport.value?.type?.name ?: "UNKNOWN"

    private suspend fun handleInboundGlobalMessage(jsonStr: String) {
        try {
            Timber.d("MESSAGING: Inbound global message — ${jsonStr.take(120)}")
            val json       = JSONObject(jsonStr)
            val type       = json.optString("type", "chat")
            val msgId      = json.getString("id")
            val senderId   = json.getString("from")
            val senderName = json.optString("name", "Unknown")
            val timestamp  = json.optLong("ts", System.currentTimeMillis())

            val actionKey = "gchat:${msgId}:${type}:${timestamp}"
            if (!seenPacketIds.add(actionKey)) return

            val existing = localDataSource.getGlobalMessageById(msgId)
            val transportTypeName = activeTransportTypeName()

            when (type) {
                "chat" -> {
                    if (existing == null) {
                        val text        = json.getString("text")
                        val replyToId   = json.optString("replyToId").takeIf { it.isNotEmpty() }
                        val replyToName = json.optString("replyToName").takeIf { it.isNotEmpty() }
                        val replyToText = json.optString("replyToText").takeIf { it.isNotEmpty() }

                        Timber.d("MESSAGE RECEIVED — type=GCHAT id=$msgId sender='$senderName' text='${text.take(40)}'")

                        localDataSource.insertGlobalMessage(
                            GlobalMessageEntity(
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
                                replyToMessageId  = replyToId,
                                replyToSenderName = replyToName,
                                replyToContent    = replyToText,
                                transportType  = transportTypeName
                            )
                        )

                        Timber.d("MESSAGE STORED — type=GCHAT id=$msgId")

                        // Show system notification for incoming global messages from peers
                        val currentUser = localDataSource.getCurrentUser().firstOrNull()
                        val localUserId = currentUser?.entityId ?: "self"
                        if (senderId != localUserId) {
                            val alert = AlertModel(
                                id = msgId,
                                type = AlertType.MESSAGE_ALERT,
                                title = "Global: $senderName",
                                description = text,
                                priority = AlertPriority.NORMAL,
                                timestamp = timestamp,
                                source = "global",
                                status = "ACTIVE"
                            )
                            notificationManager.showNotification(alert)
                        }

                        _incomingMessages.tryEmit(
                            IncomingMessage.GlobalChat(
                                messageId   = msgId,
                                senderId    = senderId,
                                senderName  = senderName,
                                text        = text,
                                timestamp   = timestamp,
                                replyToId   = replyToId,
                                replyToName = replyToName,
                                replyToText = replyToText,
                                transportType = transportTypeName
                            )
                        )
                        // Re-broadcast for mesh propagation
                        reBroadcast("GCHAT:$jsonStr")
                    }
                }
                "edit" -> {
                    val text = json.getString("text")
                    if (existing != null && timestamp > existing.updatedAt) {
                        val history = existing.editHistory.toMutableList()
                        if (!history.contains(existing.content)) history.add(existing.content)
                        localDataSource.updateGlobalMessage(
                            existing.copy(content = text, edited = true, updatedAt = timestamp, editHistory = history)
                        )
                        _incomingMessages.tryEmit(
                            IncomingMessage.GlobalChatEdit(msgId, senderId, senderName, text, timestamp)
                        )
                        reBroadcast("GCHAT:$jsonStr")
                    }
                }
                "delete" -> {
                    if (existing != null && timestamp > existing.updatedAt) {
                        localDataSource.updateGlobalMessage(
                            existing.copy(
                                content = "You deleted this message.",
                                deleted = true,
                                updatedAt = timestamp
                            )
                        )
                        _incomingMessages.tryEmit(
                            IncomingMessage.GlobalChatDelete(msgId, senderId, timestamp)
                        )
                        reBroadcast("GCHAT:$jsonStr")
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "MESSAGING: Failed to parse GCHAT packet — $jsonStr")
        }
    }

    private suspend fun handleInboundPrivateMessage(jsonStr: String) {
        try {
            Timber.d("MESSAGING: Inbound private message — ${jsonStr.take(120)}")
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
                if (!seenPacketIds.add(actionKey)) return

                val senderUser = localDataSource.getUserById(senderId)
                val senderName = senderUser?.nickname?.takeIf { it.isNotBlank() }
                    ?: senderUser?.username?.takeIf { it.isNotBlank() }
                    ?: "Contact-${senderId.take(6)}"

                val existing = localDataSource.getMessageById(msgId)
                val transportTypeName = activeTransportTypeName()

                when (type) {
                    "chat" -> {
                        if (existing == null) {
                            val text        = json.getString("text")
                            val replyToId   = json.optString("replyToId").takeIf { it.isNotEmpty() }
                            val replyToName = json.optString("replyToName").takeIf { it.isNotEmpty() }
                            val replyToText = json.optString("replyToText").takeIf { it.isNotEmpty() }

                            Timber.d("MESSAGE RECEIVED — type=MSG id=$msgId sender='$senderName' text='${text.take(40)}'")

                            localDataSource.insertMessage(
                                MessageEntity(
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
                                    replyToMessageId  = replyToId,
                                    replyToSenderName = replyToName,
                                    replyToContent    = replyToText,
                                    transportType  = transportTypeName
                                )
                            )
                            localDataSource.insertConversation(
                                ConversationEntity(
                                    entityId      = senderId,
                                    title         = senderName,
                                    lastMessageId = msgId,
                                    unreadCount   = 1,
                                    updatedAt     = timestamp
                                )
                            )

                            Timber.d("MESSAGE STORED — type=MSG id=$msgId conversationId=$senderId")

                            // Show system notification for private message from peer
                            if (senderId != localUserId) {
                                val alert = AlertModel(
                                    id = msgId,
                                    type = AlertType.MESSAGE_ALERT,
                                    title = senderName,
                                    description = text,
                                    priority = AlertPriority.HIGH,
                                    timestamp = timestamp,
                                    source = senderId,
                                    status = "ACTIVE"
                                )
                                notificationManager.showNotification(alert)
                            }

                            _incomingMessages.tryEmit(
                                IncomingMessage.PrivateChat(
                                    messageId  = msgId,
                                    senderId   = senderId,
                                    recipientId = receiverId,
                                    text       = text,
                                    timestamp  = timestamp,
                                    replyToId  = replyToId,
                                    replyToName = replyToName,
                                    replyToText = replyToText,
                                    transportType = transportTypeName
                                )
                            )
                            // Acknowledge delivery
                            sendDeliveryReceipt(msgId, localUserId, senderId)
                        }
                    }
                    "edit" -> {
                        val text = json.getString("text")
                        if (existing != null && timestamp > existing.updatedAt) {
                            val history = existing.editHistory.toMutableList()
                            if (!history.contains(existing.content)) history.add(existing.content)
                            localDataSource.insertMessage(
                                existing.copy(content = text, edited = true, updatedAt = timestamp, editHistory = history)
                            )
                        }
                    }
                    "delete" -> {
                        if (existing != null && timestamp > existing.updatedAt) {
                            localDataSource.insertMessage(
                                existing.copy(deleted = true, updatedAt = timestamp)
                            )
                        }
                    }
                    "delivery_receipt" -> {
                        if (existing != null && existing.deliveryStatus != DbDeliveryStatus.DELIVERED) {
                            localDataSource.insertMessage(existing.copy(deliveryStatus = DbDeliveryStatus.DELIVERED))
                            _incomingMessages.tryEmit(IncomingMessage.DeliveryReceipt(msgId, senderId, timestamp))
                        }
                    }
                    "read_receipt" -> {
                        if (existing != null) {
                            localDataSource.insertMessage(
                                existing.copy(deliveryStatus = DbDeliveryStatus.DELIVERED, readStatus = "READ")
                            )
                            _incomingMessages.tryEmit(IncomingMessage.ReadReceipt(msgId, senderId, timestamp))
                        }
                    }
                }
            } else {
                // Forward packet to mesh peers (mesh routing)
                val actionKey = "msg:${msgId}:${type}:${timestamp}"
                if (seenPacketIds.add(actionKey)) {
                    Timber.d("MESSAGING: Message not for us. Forwarding — id=$msgId to=$receiverId")
                    reBroadcast("MSG:$jsonStr")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "MESSAGING: Failed to parse MSG packet — $jsonStr")
        }
    }

    private suspend fun handlePeerIdentityPacket(jsonStr: String) {
        try {
            val json            = JSONObject(jsonStr)
            val remoteUserId    = json.getString("uid")
            val remoteUsername  = json.optString("un", "").ifBlank { "Contact-${remoteUserId.take(6)}" }
            val remotePublicKey = json.optString("pub", "")
            val realBleAddress  = json.optString("ble", "").takeIf { it.isNotBlank() }
            val remoteDeviceType = json.optString("dt", "SMARTPHONE")

            Timber.d("MESSAGING: Peer identity received — uid=$remoteUserId un='$remoteUsername'")
            _incomingMessages.tryEmit(
                IncomingMessage.PeerIdentity(
                    userId      = remoteUserId,
                    username    = remoteUsername,
                    publicKey   = remotePublicKey,
                    bleAddress  = realBleAddress,
                    deviceType  = remoteDeviceType
                )
            )
        } catch (e: Exception) {
            Timber.e(e, "MESSAGING: Failed to parse IDENTITY packet — $jsonStr")
        }
    }

    private fun handleReverseHandshake(str: String) {
        try {
            val json = JSONObject(str)
            val remoteUserId    = json.getString("uid")
            val remoteUsername  = json.optString("un", "").ifBlank { "Contact-${remoteUserId.take(6)}" }
            val remotePublicKey = json.optString("pub", "")
            val realBleAddress  = json.optString("ble", "").takeIf { it.isNotBlank() }
            val remoteDeviceType = json.optString("dt", "SMARTPHONE")

            Timber.d("MESSAGING: Reverse handshake received — uid=$remoteUserId")
            _incomingMessages.tryEmit(
                IncomingMessage.PeerIdentity(
                    userId     = remoteUserId,
                    username   = remoteUsername,
                    publicKey  = remotePublicKey,
                    bleAddress = realBleAddress,
                    deviceType = remoteDeviceType
                )
            )
        } catch (e: Exception) {
            Timber.e(e, "MESSAGING: Failed to parse REVERSE_HANDSHAKE — $str")
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Sends a raw protocol string packet through [CommunicationManager].
     * Used for receipts, edits, and deletions that do not require async results.
     */
    private fun sendRawPacket(packetStr: String) {
        scope.launch {
            val payload = packetStr.toByteArray(Charsets.UTF_8)
            communicationManager.sendMessage(payload)
        }
    }

    /**
     * Re-broadcasts a packet to all connected peers to propagate through the mesh.
     * Called after processing an inbound packet that was not already seen.
     */
    private fun reBroadcast(packetStr: String) {
        sendRawPacket(packetStr)
    }
}
