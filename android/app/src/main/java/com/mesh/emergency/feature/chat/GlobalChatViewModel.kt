/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.feature.chat

import androidx.lifecycle.viewModelScope
import com.mesh.emergency.core.communication.CommunicationManager
import com.mesh.emergency.core.communication.CommunicationState
import com.mesh.emergency.core.presentation.base.BaseUiEffect
import com.mesh.emergency.core.presentation.base.BaseUiEvent
import com.mesh.emergency.core.presentation.base.BaseUiState
import com.mesh.emergency.core.presentation.base.BaseViewModel
import com.mesh.emergency.data.communication.bluetooth.BluetoothTransportImpl
import com.mesh.emergency.data.local.LocalDataSource
import com.mesh.emergency.data.local.entity.GlobalMessageEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

// ── Domain model ──────────────────────────────────────────────────────────────

/**
 * UI-layer representation of a single Global Chat message.
 */
data class GlobalChatMessage(
    val id: String,
    val senderId: String,
    val senderName: String,
    val content: String,
    val timestamp: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val edited: Boolean,
    val deleted: Boolean,
    /** True when this message was sent by the local user on this device. */
    val isSelf: Boolean,
    /** SENDING | SENT | DELIVERED | QUEUED | FAILED */
    val deliveryStatus: String,
    val editHistory: List<String> = emptyList(),
    val replyToMessageId: String? = null,
    val replyToSenderName: String? = null,
    val replyToContent: String? = null
)

// ── UI State / Events / Effects ───────────────────────────────────────────────

data class GlobalChatUiState(
    val messages: List<GlobalChatMessage> = emptyList(),
    val draftText: String = "",
    val isOnline: Boolean = false,
    val isLoading: Boolean = true,
    val localUserId: String = "",
    val localUserName: String = "",
    val editingMessage: GlobalChatMessage? = null,
    val selectedMessageIds: Set<String> = emptySet(),
    val replyingToMessage: GlobalChatMessage? = null
) : BaseUiState

sealed interface GlobalChatUiEvent : BaseUiEvent {
    data class UpdateDraft(val text: String) : GlobalChatUiEvent
    data object SendMessage : GlobalChatUiEvent
    data class StartEditing(val message: GlobalChatMessage) : GlobalChatUiEvent
    data object CancelEditing : GlobalChatUiEvent
    data class EditMessage(val id: String, val newText: String) : GlobalChatUiEvent
    data class DeleteMessageForEveryone(val id: String) : GlobalChatUiEvent
    data class DeleteMessageForMe(val id: String) : GlobalChatUiEvent
    data class ToggleMessageSelection(val messageId: String) : GlobalChatUiEvent
    data object ClearSelection : GlobalChatUiEvent
    data class StartReply(val message: GlobalChatMessage) : GlobalChatUiEvent
    data object CancelReply : GlobalChatUiEvent
}

sealed interface GlobalChatUiEffect : BaseUiEffect {
    data object ScrollToBottom : GlobalChatUiEffect
    data class ShowSnackbar(val message: String) : GlobalChatUiEffect
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

/**
 * ViewModel powering the Global Chat screen.
 *
 * Architecture:
 * - Observes [LocalDataSource.getGlobalMessages] as a real-time Flow.
 * - Sends via [BluetoothTransportImpl.sendGlobalMessage] to broadcast to all BLE peers.
 * - Inserts the local user's own message optimistically to Room before BLE delivery.
 */
@HiltViewModel
class GlobalChatViewModel @Inject constructor(
    private val localDataSource: LocalDataSource,
    private val bluetoothTransport: BluetoothTransportImpl,
    private val communicationManager: CommunicationManager
) : BaseViewModel<GlobalChatUiState, GlobalChatUiEvent, GlobalChatUiEffect>(GlobalChatUiState()) {

    init {
        loadLocalProfile()
        observeMessages()
        observeConnectionState()
        viewModelScope.launch {
            try {
                localDataSource.failStuckGlobalMessages()
            } catch (e: Exception) {
                Timber.e(e, "GCHAT: Failed to sanitize stuck messages")
            }
        }
    }

    // ── Initialisation ────────────────────────────────────────────────────────

    private fun loadLocalProfile() {
        viewModelScope.launch {
            val user = localDataSource.getCurrentUser().firstOrNull()
            if (user != null) {
                val displayName = user.nickname?.takeIf { it.isNotBlank() }
                    ?: user.username.takeIf { it.isNotBlank() }
                    ?: "Me"
                updateState { copy(localUserId = user.entityId, localUserName = displayName) }
                Timber.d("GCHAT: Local profile loaded — id=${user.entityId} name='$displayName'")
            }
        }
    }

    private fun observeMessages() {
        viewModelScope.launch {
            localDataSource.getGlobalMessages().collect { entities ->
                val localId = currentState.localUserId
                val mapped = entities.map { it.toUi(localId) }
                updateState { copy(messages = mapped, isLoading = false) }
                if (mapped.isNotEmpty()) sendEffect(GlobalChatUiEffect.ScrollToBottom)
            }
        }
    }

    private fun observeConnectionState() {
        viewModelScope.launch {
            communicationManager.communicationState.collect { state ->
                val online = state == CommunicationState.CONNECTED ||
                             state == CommunicationState.SENDING ||
                             state == CommunicationState.RECEIVING
                updateState { copy(isOnline = online) }
            }
        }
    }

    // ── Event handling ────────────────────────────────────────────────────────

    override fun onEvent(event: GlobalChatUiEvent) {
        when (event) {
            is GlobalChatUiEvent.UpdateDraft -> updateState { copy(draftText = event.text) }
            GlobalChatUiEvent.SendMessage    -> {
                if (currentState.editingMessage != null) {
                    onEvent(GlobalChatUiEvent.EditMessage(currentState.editingMessage!!.id, currentState.draftText))
                } else {
                    sendCurrentDraft()
                }
            }
            is GlobalChatUiEvent.StartEditing -> {
                updateState { copy(editingMessage = event.message, draftText = event.message.content, replyingToMessage = null) }
            }
            GlobalChatUiEvent.CancelEditing -> {
                updateState { copy(editingMessage = null, draftText = "") }
            }
            is GlobalChatUiEvent.EditMessage -> {
                val text = event.newText.trim()
                if (text.isEmpty()) return
                val senderId = currentState.localUserId.ifBlank { "self" }
                val senderName = currentState.localUserName.ifBlank { "Me" }
                viewModelScope.launch {
                    val existing = localDataSource.getGlobalMessageById(event.id)
                    if (existing != null) {
                        val history = existing.editHistory.toMutableList()
                        if (!history.contains(existing.content)) {
                            history.add(existing.content)
                        }
                        val updated = existing.copy(
                            content = text,
                            edited = true,
                            updatedAt = System.currentTimeMillis(),
                            editHistory = history
                        )
                        localDataSource.insertGlobalMessage(updated)
                        
                        bluetoothTransport.sendGlobalMessageEdit(event.id, senderId, senderName, text)
                    }
                    updateState { copy(editingMessage = null, draftText = "") }
                }
            }
            is GlobalChatUiEvent.DeleteMessageForEveryone -> {
                val senderId = currentState.localUserId.ifBlank { "self" }
                val senderName = currentState.localUserName.ifBlank { "Me" }
                viewModelScope.launch {
                    val existing = localDataSource.getGlobalMessageById(event.id)
                    if (existing != null) {
                        val updated = existing.copy(
                            content = "You deleted this message.",
                            deleted = true,
                            updatedAt = System.currentTimeMillis()
                        )
                        localDataSource.insertGlobalMessage(updated)
                        
                        bluetoothTransport.sendGlobalMessageDelete(event.id, senderId, senderName)
                    }
                }
            }
            is GlobalChatUiEvent.DeleteMessageForMe -> {
                viewModelScope.launch {
                    localDataSource.deleteGlobalMessage(event.id)
                }
            }
            is GlobalChatUiEvent.ToggleMessageSelection -> {
                val current = currentState.selectedMessageIds.toMutableSet()
                if (current.contains(event.messageId)) {
                    current.remove(event.messageId)
                } else {
                    current.add(event.messageId)
                }
                updateState { copy(selectedMessageIds = current) }
            }
            GlobalChatUiEvent.ClearSelection -> {
                updateState { copy(selectedMessageIds = emptySet()) }
            }
            is GlobalChatUiEvent.StartReply -> {
                updateState { copy(replyingToMessage = event.message, editingMessage = null) }
            }
            GlobalChatUiEvent.CancelReply -> {
                updateState { copy(replyingToMessage = null) }
            }
        }
    }

    private fun sendCurrentDraft() {
        val draft = currentState.draftText.trim()
        if (draft.isEmpty()) return

        val senderId   = currentState.localUserId.ifBlank { "self" }
        val senderName = currentState.localUserName.ifBlank { "Me" }
        val msgId      = UUID.randomUUID().toString()
        val now        = System.currentTimeMillis()
        val isOnline   = currentState.isOnline

        val replyMsg   = currentState.replyingToMessage
        viewModelScope.launch {
            // Optimistic insert — message appears instantly in the list
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
                deliveryStatus = "SENDING",
                readStatus     = "READ",
                syncState      = if (isOnline) "SYNCED" else "PENDING",
                editHistory    = emptyList(),
                replyToMessageId = replyMsg?.id,
                replyToSenderName = replyMsg?.senderName,
                replyToContent = replyMsg?.content
            )
            localDataSource.insertGlobalMessage(entity)
            updateState { copy(draftText = "", replyingToMessage = null) }
            sendEffect(GlobalChatUiEffect.ScrollToBottom)

            // Always attempt BLE broadcast — transport knows if peers are connected
            val sent = bluetoothTransport.sendGlobalMessage(
                messageId  = msgId,
                senderId   = senderId,
                senderName = senderName,
                text       = draft,
                replyToId  = replyMsg?.id,
                replyToName = replyMsg?.senderName,
                replyToText = replyMsg?.content
            )
            Timber.d("GCHAT: Message sent via BLE — id=$msgId success=$sent online=$isOnline")

            // Update delivery status via dedicated UPDATE query (not insert — IGNORE would skip it)
            val finalStatus = when {
                sent     -> "SENT"
                isOnline -> "FAILED"
                else     -> "QUEUED"
            }
            localDataSource.updateGlobalMessageStatus(msgId, finalStatus)

            if (!sent && !isOnline) {
                sendEffect(GlobalChatUiEffect.ShowSnackbar("Queued — will broadcast when peers join the mesh"))
            } else if (!sent) {
                sendEffect(GlobalChatUiEffect.ShowSnackbar("Delivery failed — no peer connected"))
            }
        }
    }
}

// ── Mapper ────────────────────────────────────────────────────────────────────

private fun GlobalMessageEntity.toUi(localUserId: String) = GlobalChatMessage(
    id             = messageId,
    senderId       = senderId,
    senderName     = senderName,
    content        = content,
    timestamp      = timestamp,
    createdAt      = createdAt,
    updatedAt      = updatedAt,
    edited         = edited,
    deleted        = deleted,
    isSelf         = senderId == localUserId || senderId == "self",
    deliveryStatus = deliveryStatus,
    editHistory    = editHistory,
    replyToMessageId = replyToMessageId,
    replyToSenderName = replyToSenderName,
    replyToContent = replyToContent
)
