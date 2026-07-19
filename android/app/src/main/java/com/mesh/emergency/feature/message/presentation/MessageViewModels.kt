/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.feature.message.presentation

import androidx.lifecycle.viewModelScope
import com.mesh.emergency.core.presentation.base.BaseUiEffect
import com.mesh.emergency.core.presentation.base.BaseUiEvent
import com.mesh.emergency.core.presentation.base.BaseUiState
import com.mesh.emergency.core.presentation.base.BaseViewModel
import com.mesh.emergency.data.local.entity.DbDeliveryStatus
import com.mesh.emergency.data.local.entity.DbMessagePriority
import com.mesh.emergency.data.local.entity.DbMessageType
import com.mesh.emergency.feature.message.domain.ConversationSummary
import com.mesh.emergency.feature.message.domain.Message
import com.mesh.emergency.feature.message.domain.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

// ── Conversation List State ───────────────────────────────────────────────────
data class MessageListUiState(
    val conversations: List<ConversationSummary> = emptyList(),
    val isLoading: Boolean = true
) : BaseUiState

sealed interface MessageListUiEvent : BaseUiEvent {
    data class OpenConversation(val id: String) : MessageListUiEvent
}

sealed interface MessageListUiEffect : BaseUiEffect {
    data class NavigateToChat(val conversationId: String) : MessageListUiEffect
}

// ── Chat State ────────────────────────────────────────────────────────────────
data class ChatUiState(
    val conversationId: String = "",
    val recipientLabel: String = "Unknown",
    val messages: List<Message> = emptyList(),
    val draftText: String = "",
    val isOnline: Boolean = false,
    val pendingCount: Int = 0,
    val isLoading: Boolean = true
) : BaseUiState

sealed interface ChatUiEvent : BaseUiEvent {
    data class UpdateDraft(val text: String)  : ChatUiEvent
    data object SendMessage                   : ChatUiEvent
    data class DeleteMessage(val id: String)  : ChatUiEvent
    data class LoadConversation(val id: String, val recipientLabel: String) : ChatUiEvent
}

sealed interface ChatUiEffect : BaseUiEffect {
    data class ShowToast(val message: String) : ChatUiEffect
    data object ScrollToBottom                : ChatUiEffect
}

// ── Conversation List ViewModel ────────────────────────────────────────────────
@HiltViewModel
class MessageListViewModel @Inject constructor(
    private val messageRepository: MessageRepository
) : BaseViewModel<MessageListUiState, MessageListUiEvent, MessageListUiEffect>(MessageListUiState()) {

    init {
        observeConversations()
    }

    private fun observeConversations() {
        viewModelScope.launch {
            messageRepository.getConversations().collect { list ->
                updateState { copy(conversations = list, isLoading = false) }
            }
        }
    }

    override fun onEvent(event: MessageListUiEvent) {
        when (event) {
            is MessageListUiEvent.OpenConversation ->
                sendEffect(MessageListUiEffect.NavigateToChat(event.id))
        }
    }
}

// ── Chat ViewModel ─────────────────────────────────────────────────────────────
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val messageRepository: MessageRepository
) : BaseViewModel<ChatUiState, ChatUiEvent, ChatUiEffect>(ChatUiState()) {

    override fun onEvent(event: ChatUiEvent) {
        when (event) {
            is ChatUiEvent.LoadConversation -> loadConversation(event.id, event.recipientLabel)
            is ChatUiEvent.UpdateDraft      -> updateState { copy(draftText = event.text) }
            ChatUiEvent.SendMessage         -> sendCurrentDraft()
            is ChatUiEvent.DeleteMessage    -> {
                viewModelScope.launch {
                    messageRepository.deleteMessage(event.id)
                }
            }
        }
    }

    private fun loadConversation(id: String, recipientLabel: String) {
        updateState { copy(conversationId = id, recipientLabel = recipientLabel, isLoading = true) }
        viewModelScope.launch {
            messageRepository.getMessagesForConversation(id).collect { msgs ->
                val pending = msgs.count { it.deliveryStatus in listOf(DbDeliveryStatus.PENDING, DbDeliveryStatus.QUEUED) }
                updateState {
                    copy(messages = msgs, pendingCount = pending, isLoading = false)
                }
            }
        }
    }

    private fun sendCurrentDraft() {
        val draft = currentState.draftText.trim()
        if (draft.isEmpty()) return

        viewModelScope.launch {
            val msg = Message(
                id             = UUID.randomUUID().toString(),
                conversationId = currentState.conversationId,
                senderId       = "self",
                recipientId    = currentState.recipientLabel,
                content        = draft,
                timestamp      = System.currentTimeMillis(),
                deliveryStatus = if (currentState.isOnline) DbDeliveryStatus.SENDING else DbDeliveryStatus.QUEUED,
                type           = DbMessageType.TEXT,
                priority       = DbMessagePriority.MEDIUM,
                retryCount     = 0,
                expiryTime     = System.currentTimeMillis() + 86_400_000L
            )
            messageRepository.sendMessage(msg)
            updateState { copy(draftText = "") }
            sendEffect(ChatUiEffect.ScrollToBottom)
            if (!currentState.isOnline) {
                sendEffect(ChatUiEffect.ShowToast("Message queued — will deliver when node is reachable"))
            }
        }
    }
}
