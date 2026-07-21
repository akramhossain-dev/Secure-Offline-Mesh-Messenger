/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.feature.message.presentation

import androidx.lifecycle.viewModelScope
import com.mesh.emergency.core.messaging.MessagingService
import com.mesh.emergency.core.presentation.base.BaseUiEffect
import com.mesh.emergency.core.presentation.base.BaseUiEvent
import com.mesh.emergency.core.presentation.base.BaseUiState
import com.mesh.emergency.core.presentation.base.BaseViewModel
import com.mesh.emergency.data.local.LocalDataSource
import com.mesh.emergency.data.local.entity.DbDeliveryStatus
import com.mesh.emergency.data.local.entity.DbMessagePriority
import com.mesh.emergency.data.local.entity.DbMessageType
import com.mesh.emergency.feature.message.domain.ConversationSummary
import com.mesh.emergency.feature.message.domain.Message
import com.mesh.emergency.feature.message.domain.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.firstOrNull
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
    val recipientLabel: String = "Loading...",
    val messages: List<Message> = emptyList(),
    val draftText: String = "",
    val isOnline: Boolean = false,
    val pendingCount: Int = 0,
    val isLoading: Boolean = true,
    val editingMessage: Message? = null,
    val selectedMessageIds: Set<String> = emptySet(),
    val replyingToMessage: Message? = null
) : BaseUiState

sealed interface ChatUiEvent : BaseUiEvent {
    data class UpdateDraft(val text: String)  : ChatUiEvent
    data object SendMessage                   : ChatUiEvent
    data class StartEditing(val message: Message) : ChatUiEvent
    data object CancelEditing                 : ChatUiEvent
    data class EditMessage(val messageId: String, val newText: String) : ChatUiEvent
    data class DeleteMessageForEveryone(val id: String) : ChatUiEvent
    data class DeleteMessageForMe(val id: String) : ChatUiEvent
    data class LoadConversation(val id: String, val recipientLabel: String) : ChatUiEvent
    data class ToggleMessageSelection(val messageId: String) : ChatUiEvent
    data object ClearSelection : ChatUiEvent
    data class StartReply(val message: Message) : ChatUiEvent
    data object CancelReply : ChatUiEvent
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
    private val messageRepository: MessageRepository,
    private val communicationManager: com.mesh.emergency.core.communication.CommunicationManager,
    private val localDataSource: LocalDataSource,
    private val messagingService: MessagingService
) : BaseViewModel<ChatUiState, ChatUiEvent, ChatUiEffect>(ChatUiState()) {

    init {
        observeConnectionState()
    }

    private fun observeConnectionState() {
        viewModelScope.launch {
            communicationManager.communicationState.collect { state ->
                updateState { copy(isOnline = state == com.mesh.emergency.core.communication.CommunicationState.CONNECTED) }
            }
        }
    }

    override fun onEvent(event: ChatUiEvent) {
        when (event) {
            is ChatUiEvent.LoadConversation -> loadConversation(event.id, event.recipientLabel)
            is ChatUiEvent.UpdateDraft      -> updateState { copy(draftText = event.text) }
            ChatUiEvent.SendMessage         -> {
                if (currentState.editingMessage != null) {
                    onEvent(ChatUiEvent.EditMessage(currentState.editingMessage!!.id, currentState.draftText))
                } else {
                    sendCurrentDraft()
                }
            }
            is ChatUiEvent.StartEditing     -> {
                updateState { copy(editingMessage = event.message, draftText = event.message.content, replyingToMessage = null) }
            }
            ChatUiEvent.CancelEditing       -> {
                updateState { copy(editingMessage = null, draftText = "") }
            }
            is ChatUiEvent.EditMessage      -> {
                viewModelScope.launch {
                    val msg = messageRepository.getMessageById(event.messageId)
                    if (msg != null) {
                        val currentUser = localDataSource.getCurrentUser().firstOrNull()
                        val localUserId = currentUser?.entityId ?: "self"
                        val peerId = currentState.conversationId
                        
                        val history = msg.editHistory.toMutableList()
                        if (!history.contains(msg.content)) {
                            history.add(msg.content)
                        }
                        val updated = msg.copy(
                            content = event.newText,
                            edited = true,
                            updatedAt = System.currentTimeMillis(),
                            editHistory = history
                        )
                        messageRepository.updateMessage(updated)
                        messagingService.sendPrivateMessageEdit(event.messageId, localUserId, peerId, event.newText)
                    }
                    updateState { copy(editingMessage = null, draftText = "") }
                }
            }
            is ChatUiEvent.DeleteMessageForEveryone -> {
                viewModelScope.launch {
                    val msg = messageRepository.getMessageById(event.id)
                    if (msg != null) {
                        val currentUser = localDataSource.getCurrentUser().firstOrNull()
                        val localUserId = currentUser?.entityId ?: "self"
                        val peerId = currentState.conversationId

                        val updated = msg.copy(
                            content = "You deleted this message.",
                            deleted = true,
                            updatedAt = System.currentTimeMillis()
                        )
                        messageRepository.updateMessage(updated)
                        messagingService.sendPrivateMessageDelete(event.id, localUserId, peerId)
                    }
                }
            }
            is ChatUiEvent.DeleteMessageForMe -> {
                viewModelScope.launch {
                    messageRepository.deleteMessage(event.id)
                }
            }
            is ChatUiEvent.ToggleMessageSelection -> {
                val current = currentState.selectedMessageIds.toMutableSet()
                if (current.contains(event.messageId)) {
                    current.remove(event.messageId)
                } else {
                    current.add(event.messageId)
                }
                updateState { copy(selectedMessageIds = current) }
            }
            ChatUiEvent.ClearSelection -> {
                updateState { copy(selectedMessageIds = emptySet()) }
            }
            is ChatUiEvent.StartReply -> {
                updateState { copy(replyingToMessage = event.message, editingMessage = null) }
            }
            ChatUiEvent.CancelReply -> {
                updateState { copy(replyingToMessage = null) }
            }
        }
    }

    private fun loadConversation(id: String, recipientLabel: String) {
        updateState { copy(conversationId = id, recipientLabel = "Loading...", isLoading = true) }
        viewModelScope.launch {
            timber.log.Timber.d("PAIR_FLOW: Chat opened peerId=$id recipientLabel=$recipientLabel")
            val peerUser = localDataSource.getUserById(id)
            val dbName = peerUser?.nickname?.takeIf { it.isNotBlank() }
                ?: peerUser?.username?.takeIf { it.isNotBlank() }
            
            timber.log.Timber.d("PAIR_FLOW: Database profile query result for $id: dbName='$dbName'")
            val finalLabel = dbName
                ?: recipientLabel.takeIf { it.isNotBlank() && it != "Node" }
                ?: "Contact-${id.take(6)}"

            timber.log.Timber.d("PAIR_FLOW: Username loaded value finalLabel='$finalLabel'")
            updateState { copy(recipientLabel = finalLabel) }

            val currentUser = localDataSource.getCurrentUser().firstOrNull()
            val localUserId = currentUser?.entityId?.takeIf { it.isNotBlank() } ?: "self"
            messageRepository.getMessagesForConversation(id).collect { msgs ->
                val mappedMsgs = msgs.map { msg ->
                    msg.copy(isSelf = msg.senderId == localUserId || msg.senderId == "self")
                }
                
                // Real-time: mark received messages as read and send receipts
                val unread = mappedMsgs.filter { !it.isSelf && it.readStatus == "UNREAD" }
                for (msg in unread) {
                    messageRepository.markMessageAsRead(msg.id)
                    messagingService.sendReadReceipt(msg.id, localUserId, msg.senderId)
                }

                val pending = mappedMsgs.count { it.deliveryStatus in listOf(DbDeliveryStatus.PENDING, DbDeliveryStatus.QUEUED) }
                updateState {
                    copy(messages = mappedMsgs, pendingCount = pending, isLoading = false)
                }
            }
        }
    }

    private fun sendCurrentDraft() {
        val draft = currentState.draftText.trim()
        if (draft.isEmpty()) return

        viewModelScope.launch {
            // Use the real current-user entityId so messages persist and display correctly
            val currentUser = localDataSource.getCurrentUser().firstOrNull()
            val senderId    = currentUser?.entityId?.takeIf { it.isNotBlank() } ?: "self"

            val senderName = currentUser?.nickname?.takeIf { it.isNotBlank() }
                ?: currentUser?.username?.takeIf { it.isNotBlank() }
                ?: "Me"

            // conversationId == peer entityId (set during QR pairing)
            val peerId = currentState.conversationId

            val replyMsg = currentState.replyingToMessage
            val msg = Message(
                id             = UUID.randomUUID().toString(),
                conversationId = peerId,
                senderId       = senderId,
                senderName     = senderName,
                recipientId    = peerId,
                content        = draft,
                timestamp      = System.currentTimeMillis(),
                createdAt      = System.currentTimeMillis(),
                updatedAt      = System.currentTimeMillis(),
                edited         = false,
                deleted        = false,
                deliveryStatus = if (currentState.isOnline) DbDeliveryStatus.SENDING else DbDeliveryStatus.QUEUED,
                readStatus     = "UNREAD",
                syncState      = if (currentState.isOnline) "SYNCED" else "PENDING",
                editHistory    = emptyList(),
                type           = DbMessageType.TEXT,
                priority       = DbMessagePriority.MEDIUM,
                retryCount     = 0,
                expiryTime     = System.currentTimeMillis() + 86_400_000L,
                replyToMessageId = replyMsg?.id,
                replyToSenderName = replyMsg?.senderName,
                replyToContent = replyMsg?.content
            )
            messageRepository.sendMessage(msg)
            updateState { copy(draftText = "", replyingToMessage = null) }
            sendEffect(ChatUiEffect.ScrollToBottom)
            if (!currentState.isOnline) {
                sendEffect(ChatUiEffect.ShowToast("Message queued — will deliver when node is reachable"))
            }
        }
    }
}
