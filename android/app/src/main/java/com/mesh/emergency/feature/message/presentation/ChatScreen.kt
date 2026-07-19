/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.feature.message.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mesh.emergency.core.designsystem.component.AuroraBackdrop
import com.mesh.emergency.core.designsystem.component.GlassPanel
import com.mesh.emergency.core.designsystem.component.MeshConnectionStatus
import com.mesh.emergency.core.designsystem.theme.MeshThemeTokens
import com.mesh.emergency.data.local.entity.DbDeliveryStatus
import com.mesh.emergency.feature.message.domain.Message
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Offline Chat screen — shows message bubbles for a single conversation,
 * with message input and delivery status indicators.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversationId: String,
    recipientLabel: String,
    onBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val spacing = MeshThemeTokens.spacing
    val semanticColors = MeshThemeTokens.semanticColors

    // Load conversation on first composition
    LaunchedEffect(conversationId) {
        viewModel.onEvent(ChatUiEvent.LoadConversation(conversationId, recipientLabel))
    }

    // Handle effects
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ChatUiEffect.ShowToast -> snackbarHostState.showSnackbar(effect.message)
                ChatUiEffect.ScrollToBottom -> {
                    if (uiState.messages.isNotEmpty()) {
                        listState.animateScrollToItem(uiState.messages.lastIndex)
                    }
                }
            }
        }
    }

    // Auto-scroll on new messages
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }

    AuroraBackdrop(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(
                                    text = uiState.recipientLabel.ifBlank { recipientLabel },
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = if (uiState.isOnline) "Online" else "Offline — queue active",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (uiState.isOnline) semanticColors.connected else semanticColors.warning
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Default.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    actions = {
                        MeshConnectionStatus(isConnected = uiState.isOnline)
                        Spacer(Modifier.width(8.dp))
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            bottomBar = {
                MessageInputBar(
                    draft = uiState.draftText,
                    isOnline = uiState.isOnline,
                    pendingCount = uiState.pendingCount,
                    onDraftChange = { viewModel.onEvent(ChatUiEvent.UpdateDraft(it)) },
                    onSend = { viewModel.onEvent(ChatUiEvent.SendMessage) }
                )
            }
        ) { paddingValues ->
            if (uiState.messages.isEmpty()) {
                // ── Empty Chat ─────────────────────────────────────────────────
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "No messages yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Messages will be queued offline until node is reachable",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(
                        start = spacing.lg, end = spacing.lg,
                        top = paddingValues.calculateTopPadding() + spacing.sm,
                        bottom = paddingValues.calculateBottomPadding() + spacing.sm
                    ),
                    verticalArrangement = Arrangement.spacedBy(spacing.sm),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.messages, key = { it.id }) { message ->
                        MessageBubble(
                            message = message,
                            isSelf = message.isSelf
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Message Bubble
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MessageBubble(message: Message, isSelf: Boolean) {
    val semanticColors = MeshThemeTokens.semanticColors

    Row(
        horizontalArrangement = if (isSelf) Arrangement.End else Arrangement.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = if (isSelf) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            // Sender label for received messages
            if (!isSelf) {
                Text(
                    text = message.senderId,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                )
            }

            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp, topEnd = 16.dp,
                            bottomStart = if (isSelf) 16.dp else 4.dp,
                            bottomEnd = if (isSelf) 4.dp else 16.dp
                        )
                    )
                    .background(
                        if (isSelf) MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                        else MaterialTheme.colorScheme.surface.copy(alpha = 0.2f)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Column {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isSelf) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Timestamp + delivery status row
            Row(
                horizontalArrangement = if (isSelf) Arrangement.End else Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
            ) {
                Text(
                    text = formatChatTime(message.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isSelf) {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = deliveryStatusLabel(message.deliveryStatus),
                        style = MaterialTheme.typography.labelSmall,
                        color = deliveryStatusColor(message.deliveryStatus)
                    )
                }
                // Priority indicator for CRITICAL/HIGH messages
                if (message.priority in listOf(
                        com.mesh.emergency.data.local.entity.DbMessagePriority.CRITICAL,
                        com.mesh.emergency.data.local.entity.DbMessagePriority.HIGH
                    )
                ) {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "!",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = priorityColor(message.priority)
                    )
                }
            }

            // Retry indicator
            if (message.retryCount > 0 && isSelf) {
                Text(
                    text = "Retried ${message.retryCount}×",
                    style = MaterialTheme.typography.labelSmall,
                    color = semanticColors.warning,
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 2.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Message Input Bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MessageInputBar(
    draft: String,
    isOnline: Boolean,
    pendingCount: Int,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit
) {
    val semanticColors = MeshThemeTokens.semanticColors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Pending count banner
        if (pendingCount > 0) {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "📦 $pendingCount message(s) queued — will send when node is reachable",
                    style = MaterialTheme.typography.labelSmall,
                    color = semanticColors.warning
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChange,
                placeholder = {
                    Text(
                        text = if (isOnline) "Message…" else "Message (offline queue)…",
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.weight(1f),
                singleLine = false,
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = onSend,
                enabled = draft.isNotBlank(),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = if (draft.isNotBlank())
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatChatTime(ts: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ts))
