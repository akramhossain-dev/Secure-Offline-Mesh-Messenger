/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.messaging

import com.mesh.emergency.core.common.result.Result
import kotlinx.coroutines.flow.Flow

/**
 * Transport-agnostic messaging layer contract.
 *
 * The UI and ViewModel layers depend exclusively on this interface.
 * Changing the underlying transport (Bluetooth → LoRa → Wi-Fi Direct)
 * requires only swapping the [CommunicationManager]'s active transport —
 * no UI or ViewModel changes needed.
 *
 * Responsibilities:
 * - Global broadcast messages (send, edit, delete)
 * - Private peer-to-peer messages (send, edit, delete)
 * - Delivery receipts and read receipts
 * - Observing inbound messages from all active transports
 */
interface MessagingService {

    // ── Global Chat ───────────────────────────────────────────────────────────

    /**
     * Broadcasts a new global chat message to all connected mesh peers.
     *
     * @return true if the message was dispatched to at least one peer.
     */
    suspend fun sendGlobalMessage(
        messageId: String,
        senderId: String,
        senderName: String,
        text: String,
        replyToId: String? = null,
        replyToName: String? = null,
        replyToText: String? = null
    ): Boolean

    /**
     * Broadcasts an edit to an existing global message.
     *
     * @return true if dispatched successfully.
     */
    suspend fun sendGlobalMessageEdit(
        messageId: String,
        senderId: String,
        senderName: String,
        newText: String
    ): Boolean

    /**
     * Broadcasts a delete event for an existing global message.
     *
     * @return true if dispatched successfully.
     */
    suspend fun sendGlobalMessageDelete(
        messageId: String,
        senderId: String,
        senderName: String
    ): Boolean

    // ── Private Messaging ─────────────────────────────────────────────────────

    /**
     * Sends a private chat message to [recipientId].
     *
     * @return true if dispatched to transport successfully.
     */
    suspend fun sendPrivateMessage(
        messageId: String,
        senderId: String,
        senderName: String,
        recipientId: String,
        text: String,
        replyToId: String? = null,
        replyToName: String? = null,
        replyToText: String? = null
    ): Boolean

    /**
     * Sends an edit update for an existing private message.
     * Routed to the specific recipient identified by [recipientId].
     */
    fun sendPrivateMessageEdit(
        messageId: String,
        senderId: String,
        recipientId: String,
        newText: String
    )

    /**
     * Sends a delete event for an existing private message.
     */
    fun sendPrivateMessageDelete(
        messageId: String,
        senderId: String,
        recipientId: String
    )

    // ── Receipts ──────────────────────────────────────────────────────────────

    /**
     * Sends a read receipt to [recipientId] acknowledging [messageId] was read.
     */
    fun sendReadReceipt(
        messageId: String,
        senderId: String,
        recipientId: String
    )

    /**
     * Sends a delivery receipt to [recipientId] confirming [messageId] was received.
     */
    fun sendDeliveryReceipt(
        messageId: String,
        senderId: String,
        recipientId: String
    )

    // ── Inbound Observation ───────────────────────────────────────────────────

    /**
     * Emits all inbound messages received across any active transport.
     * The messaging layer handles protocol parsing; consumers receive structured events.
     */
    fun observeIncomingMessages(): Flow<IncomingMessage>

    // ── Queue Management ──────────────────────────────────────────────────────

    /**
     * Drains queued outbound messages to all connected peers.
     * Called automatically when a peer connects or transport state changes.
     */
    suspend fun drainQueue()

    /**
     * Syncs recent message history to a newly connected peer.
     *
     * @param remoteUserId The entityId of the peer that just connected.
     */
    suspend fun onPeerConnected(remoteUserId: String)
}

// ── Inbound Message Types ─────────────────────────────────────────────────────

/**
 * Sealed hierarchy representing inbound message events decoded by [MessagingService].
 * All consumers (ViewModels, workers) should react to these typed events.
 */
sealed interface IncomingMessage {

    /** A new global (broadcast) message arrived. */
    data class GlobalChat(
        val messageId: String,
        val senderId: String,
        val senderName: String,
        val text: String,
        val timestamp: Long,
        val replyToId: String? = null,
        val replyToName: String? = null,
        val replyToText: String? = null,
        /** Which transport delivered this message. */
        val transportType: String = "UNKNOWN"
    ) : IncomingMessage

    /** An edit to an existing global message arrived. */
    data class GlobalChatEdit(
        val messageId: String,
        val senderId: String,
        val senderName: String,
        val newText: String,
        val timestamp: Long
    ) : IncomingMessage

    /** A delete event for an existing global message arrived. */
    data class GlobalChatDelete(
        val messageId: String,
        val senderId: String,
        val timestamp: Long
    ) : IncomingMessage

    /** A new private message arrived. */
    data class PrivateChat(
        val messageId: String,
        val senderId: String,
        val recipientId: String,
        val text: String,
        val timestamp: Long,
        val replyToId: String? = null,
        val replyToName: String? = null,
        val replyToText: String? = null,
        val transportType: String = "UNKNOWN"
    ) : IncomingMessage

    /** A delivery receipt arrived (the recipient confirmed receipt). */
    data class DeliveryReceipt(
        val messageId: String,
        val senderId: String,
        val timestamp: Long
    ) : IncomingMessage

    /** A read receipt arrived (the recipient read the message). */
    data class ReadReceipt(
        val messageId: String,
        val senderId: String,
        val timestamp: Long
    ) : IncomingMessage

    /** A peer identity handshake arrived (for auto-pairing). */
    data class PeerIdentity(
        val userId: String,
        val username: String,
        val publicKey: String,
        val bleAddress: String?,
        val deviceType: String
    ) : IncomingMessage
}
