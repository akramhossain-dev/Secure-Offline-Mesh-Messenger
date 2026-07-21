/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.feature.overlay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mesh.emergency.core.communication.CommunicationManager
import com.mesh.emergency.core.communication.CommunicationState
import com.mesh.emergency.core.messaging.MessagingService
import com.mesh.emergency.data.local.LocalDataSource
import com.mesh.emergency.data.local.entity.DbDeliveryStatus
import com.mesh.emergency.data.local.entity.DbMessagePriority
import com.mesh.emergency.data.local.entity.DbMessageType
import com.mesh.emergency.data.local.entity.GlobalMessageEntity
import com.mesh.emergency.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID


// ── UI Models ──────────────────────────────────────────────────────────────────

data class OverlayMessage(
    val id: String,
    val senderId: String,
    val senderName: String,
    val content: String,
    val timestamp: Long,
    val isSelf: Boolean,
    val deliveryStatus: String = "SENT" // "SENDING" | "SENT" | "DELIVERED" | "READ" | "QUEUED" | "FAILED"
)

data class OverlayUiState(
    val convId: String = "",
    val peerName: String = "Chat",
    val isOnline: Boolean = false,
    val localUserId: String = "",
    val localUserName: String = "",
    val draftText: String = "",
    val messages: List<OverlayMessage> = emptyList(),
    val unreadCount: Int = 0,
    val isSending: Boolean = false
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

/**
 * ViewModel for the floating overlay conversation popup.
 * Created via a manual ViewModelProvider.Factory inside ChatHeadService,
 * which already has all dependencies Hilt-injected. One instance per conversation.
 */
class OverlayConversationViewModel(
    private val localDataSource: LocalDataSource,
    private val messagingService: MessagingService,
    private val communicationManager: CommunicationManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(OverlayUiState())
    val uiState: StateFlow<OverlayUiState> = _uiState.asStateFlow()

    private var initialized = false

    /**
     * Must be called exactly once after creation.
     * ChatHeadService calls this after obtaining the ViewModel from ViewModelProvider.
     */
    fun init(convId: String, peerName: String) {
        if (initialized) return
        initialized = true
        val resolvedTitle = if (convId == "global") "Global Mesh Chat" else peerName
        _uiState.update { it.copy(convId = convId, peerName = resolvedTitle) }
        loadLocalProfile(convId)
        observeConnectionState()
        observeMessages(convId)
    }

    // ── Private Setup ─────────────────────────────────────────────────────────

    private fun loadLocalProfile(convId: String) {
        viewModelScope.launch {
            try {
                val user = localDataSource.getCurrentUser().firstOrNull()
                if (user != null) {
                    val name = user.nickname?.takeIf { it.isNotBlank() }
                        ?: user.username.takeIf { it.isNotBlank() }
                        ?: "Me"
                    _uiState.update { it.copy(localUserId = user.entityId, localUserName = name) }
                }
            } catch (e: Exception) {
                Timber.e(e, "OVERLAY_VM[$convId]: Failed to load local profile")
            }
        }
    }

    private fun observeConnectionState() {
        viewModelScope.launch {
            communicationManager.communicationState.collect { state ->
                val online = state == CommunicationState.CONNECTED ||
                        state == CommunicationState.SENDING ||
                        state == CommunicationState.RECEIVING
                _uiState.update { it.copy(isOnline = online) }
            }
        }
    }

    private fun observeMessages(convId: String) {
        viewModelScope.launch {
            localDataSource.clearUnreadCount(convId)
            if (convId == "global") {
                localDataSource.getGlobalMessages().collect { entities ->
                    val localId = _uiState.value.localUserId
                    val mapped = entities.map { msg ->
                        OverlayMessage(
                            id             = msg.messageId,
                            senderId       = msg.senderId,
                            senderName     = msg.senderName,
                            content        = msg.content,
                            timestamp      = msg.timestamp,
                            isSelf         = msg.senderId == localId || msg.senderId == "self",
                            deliveryStatus = msg.deliveryStatus
                        )
                    }
                    _uiState.update { it.copy(messages = mapped) }
                }
            } else {
                localDataSource.getMessagesForConversation(convId).collect { entities ->
                    val localId = _uiState.value.localUserId
                    val mapped = entities.map { msg ->
                        val statusStr = if (msg.readStatus == "READ") "READ" else msg.deliveryStatus.name
                        OverlayMessage(
                            id             = msg.messageId,
                            senderId       = msg.senderId,
                            senderName     = msg.senderName,
                            content        = msg.content,
                            timestamp      = msg.timestamp,
                            isSelf         = msg.senderId == localId || msg.senderId == "self",
                            deliveryStatus = statusStr
                        )
                    }
                    _uiState.update { it.copy(messages = mapped) }
                }
            }
        }
    }

    // ── User Actions ──────────────────────────────────────────────────────────

    fun updateDraft(text: String) {
        _uiState.update { it.copy(draftText = text) }
    }

    fun incrementUnread() {
        _uiState.update { it.copy(unreadCount = it.unreadCount + 1) }
    }

    fun clearUnread() {
        _uiState.update { it.copy(unreadCount = 0) }
    }

    fun sendMessage() {
        val draft = _uiState.value.draftText.trim()
        if (draft.isEmpty() || _uiState.value.isSending) return

        _uiState.update { it.copy(draftText = "", isSending = true) }

        viewModelScope.launch {
            try {
                // Ensure local profile is loaded before sending
                var state = _uiState.value
                if (state.localUserId.isBlank()) {
                    val user = localDataSource.getCurrentUser().firstOrNull()
                    if (user != null) {
                        val name = user.nickname?.takeIf { it.isNotBlank() }
                            ?: user.username.takeIf { it.isNotBlank() }
                            ?: "Me"
                        _uiState.update { it.copy(localUserId = user.entityId, localUserName = name) }
                        state = _uiState.value
                    }
                }

                val senderId   = state.localUserId.ifBlank { "self" }
                val senderName = state.localUserName.ifBlank { "Me" }
                val msgId      = UUID.randomUUID().toString()
                val now        = System.currentTimeMillis()
                val convId     = state.convId

                Timber.d("OVERLAY_VM[$convId]: sendMessage — sender=$senderId name=$senderName text=${draft.take(30)}")

                if (convId == "global") {
                    val entity = GlobalMessageEntity(
                        messageId      = msgId,
                        senderId       = senderId,
                        senderName     = senderName,
                        content        = draft,
                        timestamp      = now,
                        createdAt      = now,
                        updatedAt      = now,
                        edited         = false,
                        deleted        = false,
                        deliveryStatus = "SENT",
                        readStatus     = "READ",
                        syncState      = "SYNCED"
                    )
                    localDataSource.insertGlobalMessage(entity)
                    val result = messagingService.sendGlobalMessage(msgId, senderId, senderName, draft)
                    Timber.d("OVERLAY_VM[$convId]: sendGlobalMessage result=$result")
                } else {
                    val entity = MessageEntity(
                        entityId       = msgId,
                        messageId      = msgId,
                        conversationId = convId,
                        senderId       = senderId,
                        senderName     = senderName,
                        recipientId    = convId,
                        content        = draft,
                        timestamp      = now,
                        createdAt      = now,
                        updatedAt      = now,
                        edited         = false,
                        deleted        = false,
                        deliveryStatus = DbDeliveryStatus.SENT,
                        readStatus     = "READ",
                        syncState      = "SYNCED",
                        type           = DbMessageType.TEXT,
                        priority       = DbMessagePriority.MEDIUM,
                        expiryTime     = now + 86_400_000L,
                        retryCount     = 0
                    )
                    val sent = messagingService.sendPrivateMessage(
                        messageId   = msgId,
                        senderId    = senderId,
                        senderName  = senderName,
                        recipientId = convId,
                        text        = draft
                    )

                    val finalStatus = if (sent) DbDeliveryStatus.SENT else DbDeliveryStatus.QUEUED
                    localDataSource.insertMessage(
                        entity.copy(
                            deliveryStatus = finalStatus,
                            syncState      = if (sent) "SYNCED" else "PENDING"
                        )
                    )
                }
                Timber.d("OVERLAY_VM[$convId]: Message sent — id=$msgId")
            } catch (e: Exception) {
                Timber.e(e, "OVERLAY_VM: Failed to send message — ${e.message}")
                // Restore draft on failure so user can retry
                _uiState.update { it.copy(draftText = draft) }
            } finally {
                _uiState.update { it.copy(isSending = false) }
            }
        }
    }
}

