/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.feature.chat

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mesh.emergency.core.designsystem.component.*
import com.mesh.emergency.core.designsystem.theme.MeshThemeTokens

/**
 * Global Chat Screen — supports mesh-wide message broadcasting, day separators,
 * context actions (long press), dynamic time formats, and elegant animations.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GlobalChatScreen(
    onBack: () -> Unit,
    viewModel: GlobalChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val spacing = MeshThemeTokens.spacing
    val semanticColors = MeshThemeTokens.semanticColors
    val context = LocalContext.current

    var selectedMessageForMenu by remember { mutableStateOf<GlobalChatMessage?>(null) }

    // Handle effects
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is GlobalChatUiEffect.ScrollToBottom -> {
                    if (uiState.messages.isNotEmpty()) {
                        listState.animateScrollToItem(uiState.messages.lastIndex)
                    }
                }
                is GlobalChatUiEffect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(effect.message)
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
            contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Global Mesh Chat",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = if (uiState.isOnline) "Mesh active" else "Searching peers…",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (uiState.isOnline) semanticColors.connected else semanticColors.warning
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Default.ArrowBack,
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
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = paddingValues.calculateTopPadding())
                    .imePadding()
            ) {
                // Connection indicator
                if (!uiState.isOnline) {
                    GlassPanel(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.WifiOff,
                                contentDescription = null,
                                tint = semanticColors.warning,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "No mesh connection — messages queued locally",
                                style = MaterialTheme.typography.labelSmall,
                                color = semanticColors.warning
                            )
                        }
                    }
                }

                // Message list
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when {
                        uiState.isLoading -> GlobalChatLoadingState()
                        uiState.messages.isEmpty() -> GlobalChatEmptyState()
                        else -> {
                            LazyColumn(
                                state = listState,
                                contentPadding = PaddingValues(
                                    start = spacing.lg,
                                    end = spacing.lg,
                                    top = spacing.sm,
                                    bottom = spacing.sm
                                ),
                                verticalArrangement = Arrangement.spacedBy(spacing.sm),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                itemsIndexed(uiState.messages, key = { _, message -> message.id }) { index, message ->
                                    val showDateSeparator = index == 0 || !com.mesh.emergency.core.utils.DateUtils.isSameDay(
                                        uiState.messages[index - 1].timestamp,
                                        message.timestamp
                                    )
                                    Column(modifier = Modifier.animateItemPlacement()) {
                                        if (showDateSeparator) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 12.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                                        .padding(horizontal = 12.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        text = com.mesh.emergency.core.utils.DateUtils.formatRelativeDay(message.timestamp),
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }

                                        GlobalMessageBubble(
                                            message = message,
                                            onLongClick = { selectedMessageForMenu = message }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Input bar
                GlobalChatInputBar(
                    draft = uiState.draftText,
                    isOnline = uiState.isOnline,
                    editingMessage = uiState.editingMessage,
                    onDraftChange = { viewModel.onEvent(GlobalChatUiEvent.UpdateDraft(it)) },
                    onSend = { viewModel.onEvent(GlobalChatUiEvent.SendMessage) },
                    onCancelEdit = { viewModel.onEvent(GlobalChatUiEvent.CancelEditing) }
                )
            }
        }
    }

    if (selectedMessageForMenu != null) {
        GlobalMessageActionsDialog(
            message = selectedMessageForMenu!!,
            onDismiss = { selectedMessageForMenu = null },
            onReply = {
                Toast.makeText(context, "Reply functionality prepared.", Toast.LENGTH_SHORT).show()
                selectedMessageForMenu = null
            },
            onEdit = {
                viewModel.onEvent(GlobalChatUiEvent.StartEditing(selectedMessageForMenu!!))
                selectedMessageForMenu = null
            },
            onDeleteForEveryone = {
                viewModel.onEvent(GlobalChatUiEvent.DeleteMessageForEveryone(selectedMessageForMenu!!.id))
                selectedMessageForMenu = null
            },
            onDeleteForMe = {
                viewModel.onEvent(GlobalChatUiEvent.DeleteMessageForMe(selectedMessageForMenu!!.id))
                selectedMessageForMenu = null
            },
            onCopy = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("message", selectedMessageForMenu!!.content)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                selectedMessageForMenu = null
            },
            onForward = {
                Toast.makeText(context, "Forwarding: choose peer.", Toast.LENGTH_SHORT).show()
                selectedMessageForMenu = null
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Global Message Actions Dialog
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun GlobalMessageActionsDialog(
    message: GlobalChatMessage,
    onDismiss: () -> Unit,
    onReply: () -> Unit,
    onEdit: () -> Unit,
    onDeleteForEveryone: () -> Unit,
    onDeleteForMe: () -> Unit,
    onCopy: () -> Unit,
    onForward: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Message Actions",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                DropdownMenuItem(
                    text = { Text("Copy Text") },
                    onClick = onCopy,
                    leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text("Reply") },
                    onClick = onReply,
                    leadingIcon = { Icon(Icons.Default.Reply, contentDescription = null) }
                )
                if (message.isSelf && !message.deleted) {
                    DropdownMenuItem(
                        text = { Text("Edit Message") },
                        onClick = onEdit,
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                    )
                }
                if (message.isSelf && !message.deleted) {
                    DropdownMenuItem(
                        text = { Text("Delete for Everyone") },
                        onClick = onDeleteForEveryone,
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Delete for Me") },
                    onClick = onDeleteForMe,
                    leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                )
                DropdownMenuItem(
                    text = { Text("Forward Message") },
                    onClick = onForward,
                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Message Bubble
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GlobalMessageBubble(
    message: GlobalChatMessage,
    onLongClick: () -> Unit
) {
    val isSelf = message.isSelf
    val context = LocalContext.current

    Row(
        horizontalArrangement = if (isSelf) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        if (!isSelf) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = message.senderName.take(1).uppercase(),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isSelf) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            if (!isSelf) {
                Text(
                    text = message.senderName,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                )
            }

            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 18.dp, topEnd = 18.dp,
                            bottomStart = if (isSelf) 18.dp else 4.dp,
                            bottomEnd = if (isSelf) 4.dp else 18.dp
                        )
                    )
                    .background(
                        if (isSelf) MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                    )
                    .combinedClickable(
                        onLongClick = onLongClick,
                        onClick = {}
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Column {
                    val displayContent = when {
                        message.deleted -> if (isSelf) "You deleted this message." else "${message.senderName} deleted this message."
                        else -> message.content
                    }
                    val textStyle = if (message.deleted) {
                        MaterialTheme.typography.bodyMedium.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                    } else {
                        MaterialTheme.typography.bodyMedium
                    }
                    val textColor = if (message.deleted) {
                        if (isSelf) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    } else {
                        if (isSelf) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface
                    }

                    Text(
                        text = displayContent,
                        style = textStyle,
                        color = textColor
                    )
                }
            }

            Row(
                horizontalArrangement = if (isSelf) Arrangement.End else Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
            ) {
                val timeText = com.mesh.emergency.core.utils.DateUtils.formatMessageTime(context, message.timestamp)
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (message.edited && !message.deleted) {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Edited",
                        style = MaterialTheme.typography.labelSmall.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isSelf && !message.deleted) {
                    Spacer(Modifier.width(4.dp))
                    
                    val statusIcon: androidx.compose.ui.graphics.vector.ImageVector
                    val statusColor: Color
                    when (message.deliveryStatus.uppercase()) {
                        "SENDING", "QUEUED" -> {
                            statusIcon = Icons.Default.Schedule
                            statusColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        }
                        "SENT", "DELIVERED" -> {
                            statusIcon = Icons.Default.DoneAll
                            statusColor = MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        else -> {
                            statusIcon = Icons.Default.Error
                            statusColor = MaterialTheme.colorScheme.error
                        }
                    }
                    
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Input Bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun GlobalChatInputBar(
    draft: String,
    isOnline: Boolean,
    editingMessage: GlobalChatMessage?,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onCancelEdit: () -> Unit
) {
    val isEditing = editingMessage != null
    val sendButtonScale by animateFloatAsState(
        targetValue = if (draft.isNotBlank()) 1f else 0.8f,
        animationSpec = tween(200),
        label = "SendButtonScale"
    )

    Column(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)
    ) {
        if (isEditing) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Edit Broadcast",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = editingMessage!!.content,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
                IconButton(onClick = onCancelEdit, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            IconButton(onClick = {}, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Attachment",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = {}, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.Face,
                    contentDescription = "Emoji",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(4.dp))

            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChange,
                placeholder = {
                    Text(
                        text = if (isOnline) "Broadcast to all nodes…" else "Message (offline queue)…",
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.weight(1f),
                singleLine = false,
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )
            Spacer(Modifier.width(4.dp))
            IconButton(
                onClick = onSend,
                enabled = draft.isNotBlank(),
                modifier = Modifier.size(44.dp).scale(sendButtonScale)
            ) {
                Icon(
                    imageVector = if (isEditing) Icons.Default.Check else Icons.Default.Send,
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

// ─────────────────────────────────────────────────────────────────────────────
// Empty State
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun GlobalChatEmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(text = "🌐", style = MaterialTheme.typography.displaySmall)
            Text(
                text = "No messages yet",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Be the first to broadcast to the mesh!\nAll connected nodes will receive your message.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Loading State
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun GlobalChatLoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            androidx.compose.material3.CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Loading messages…",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
