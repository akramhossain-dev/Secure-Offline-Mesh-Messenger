/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.feature.chat

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mesh.emergency.core.designsystem.component.*
import com.mesh.emergency.core.designsystem.theme.MeshThemeTokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    val scope = rememberCoroutineScope()

    var activeMessageForSheet by remember { mutableStateOf<GlobalChatMessage?>(null) }
    var isSheetOpen by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Highlight State
    var highlightedMessageId by remember { mutableStateOf<String?>(null) }

    // Focus Requester to coordinate soft keyboard
    val focusRequester = remember { FocusRequester() }

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

    // Coordinate text field focus on edit/reply modes
    LaunchedEffect(uiState.replyingToMessage, uiState.editingMessage) {
        if (uiState.replyingToMessage != null || uiState.editingMessage != null) {
            delay(100)
            focusRequester.requestFocus()
        }
    }

    val isSelectionMode = uiState.selectedMessageIds.isNotEmpty()

    AuroraBackdrop(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                if (isSelectionMode) {
                    TopAppBar(
                        title = {
                            Text(
                                text = "${uiState.selectedMessageIds.size} Selected",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { viewModel.onEvent(GlobalChatUiEvent.ClearSelection) }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear Selection")
                            }
                        },
                        actions = {
                            if (uiState.selectedMessageIds.size == 1) {
                                val selectedMsg = uiState.messages.firstOrNull { it.id in uiState.selectedMessageIds }
                                if (selectedMsg != null && selectedMsg.isSelf && !selectedMsg.deleted) {
                                    IconButton(onClick = {
                                        viewModel.onEvent(GlobalChatUiEvent.StartEditing(selectedMsg))
                                        viewModel.onEvent(GlobalChatUiEvent.ClearSelection)
                                    }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                                    }
                                }
                                IconButton(onClick = {
                                    selectedMsg?.let {
                                        viewModel.onEvent(GlobalChatUiEvent.StartReply(it))
                                    }
                                    viewModel.onEvent(GlobalChatUiEvent.ClearSelection)
                                }) {
                                    Icon(Icons.AutoMirrored.Default.Reply, contentDescription = "Reply")
                                }
                            }
                            IconButton(onClick = {
                                val selectedMsgs = uiState.messages.filter { it.id in uiState.selectedMessageIds }
                                val textToCopy = selectedMsgs.joinToString("\n") { it.content }
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("global_messages", textToCopy)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Copied selected to clipboard", Toast.LENGTH_SHORT).show()
                                viewModel.onEvent(GlobalChatUiEvent.ClearSelection)
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                            }
                            IconButton(onClick = {
                                uiState.selectedMessageIds.forEach { id ->
                                    viewModel.onEvent(GlobalChatUiEvent.DeleteMessageForMe(id))
                                }
                                viewModel.onEvent(GlobalChatUiEvent.ClearSelection)
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete for Me", tint = MaterialTheme.colorScheme.error)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                        )
                    )
                } else {
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
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = paddingValues.calculateTopPadding())
                    .imePadding()
            ) {
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
                                    start = spacing.md,
                                    end = spacing.md,
                                    top = spacing.sm,
                                    bottom = spacing.sm
                                ),
                                verticalArrangement = Arrangement.spacedBy(spacing.xs),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                itemsIndexed(uiState.messages, key = { index, message -> if (message.id.isNotBlank()) message.id else "global_msg_$index" }) { index, message ->
                                    val showDateSeparator = index == 0 || !com.mesh.emergency.core.utils.DateUtils.isSameDay(
                                        uiState.messages[index - 1].timestamp,
                                        message.timestamp
                                    )
                                    val isSelected = message.id in uiState.selectedMessageIds

                                    Column(modifier = Modifier.animateItemPlacement()) {
                                        if (showDateSeparator) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 16.dp)
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

                                        SwipeToReplyBox(
                                            onReply = {
                                                viewModel.onEvent(GlobalChatUiEvent.StartReply(message))
                                            }
                                        ) {
                                            GlobalMessageBubble(
                                                message = message,
                                                isSelected = isSelected,
                                                highlightedMessageId = highlightedMessageId,
                                                onClick = {
                                                    if (isSelectionMode) {
                                                        viewModel.onEvent(GlobalChatUiEvent.ToggleMessageSelection(message.id))
                                                    } else {
                                                        activeMessageForSheet = message
                                                        isSheetOpen = true
                                                    }
                                                },
                                                onLongClick = {
                                                    viewModel.onEvent(GlobalChatUiEvent.ToggleMessageSelection(message.id))
                                                },
                                                onReplyClick = { replyToId ->
                                                    val targetIdx = uiState.messages.indexOfFirst { it.id == replyToId }
                                                    if (targetIdx != -1) {
                                                        val targetMsg = uiState.messages[targetIdx]
                                                        if (targetMsg.deleted) {
                                                            Toast.makeText(context, "Original message unavailable.", Toast.LENGTH_SHORT).show()
                                                        } else {
                                                            scope.launch {
                                                                listState.animateScrollToItem(targetIdx)
                                                                highlightedMessageId = replyToId
                                                                delay(1000)
                                                                if (highlightedMessageId == replyToId) {
                                                                    highlightedMessageId = null
                                                                }
                                                            }
                                                        }
                                                    } else {
                                                        Toast.makeText(context, "Original message unavailable.", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                GlobalChatInputBar(
                    draft = uiState.draftText,
                    isOnline = uiState.isOnline,
                    editingMessage = uiState.editingMessage,
                    replyingToMessage = uiState.replyingToMessage,
                    focusRequester = focusRequester,
                    onDraftChange = { viewModel.onEvent(GlobalChatUiEvent.UpdateDraft(it)) },
                    onSend = { viewModel.onEvent(GlobalChatUiEvent.SendMessage) },
                    onCancelEdit = { viewModel.onEvent(GlobalChatUiEvent.CancelEditing) },
                    onCancelReply = { viewModel.onEvent(GlobalChatUiEvent.CancelReply) }
                )
            }
        }
    }

    if (isSheetOpen && activeMessageForSheet != null) {
        ModalBottomSheet(
            onDismissRequest = { isSheetOpen = false },
            sheetState = sheetState,
            dragHandle = { Box(modifier = Modifier.padding(vertical = 8.dp).width(36.dp).height(4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))) },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            val msg = activeMessageForSheet!!
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Quick Reactions Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("👍", "❤️", "😂", "😮", "😢", "🙏").forEach { emoji ->
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    Toast.makeText(context, "Reaction $emoji recorded", Toast.LENGTH_SHORT).show()
                                    isSheetOpen = false
                                }
                        ) {
                            Text(text = emoji, fontSize = 20.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                BottomSheetActionItem(
                    icon = Icons.AutoMirrored.Default.Reply,
                    label = "Reply",
                    onClick = {
                        viewModel.onEvent(GlobalChatUiEvent.StartReply(msg))
                        isSheetOpen = false
                    }
                )

                BottomSheetActionItem(
                    icon = Icons.Default.ContentCopy,
                    label = "Copy Text",
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("message", msg.content)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        isSheetOpen = false
                    }
                )

                if (msg.isSelf && !msg.deleted) {
                    BottomSheetActionItem(
                        icon = Icons.Default.Edit,
                        label = "Edit Message",
                        onClick = {
                            viewModel.onEvent(GlobalChatUiEvent.StartEditing(msg))
                            isSheetOpen = false
                        }
                    )
                }

                if (msg.isSelf && !msg.deleted) {
                    BottomSheetActionItem(
                        icon = Icons.Default.Delete,
                        label = "Delete for Everyone",
                        textColor = MaterialTheme.colorScheme.error,
                        onClick = {
                            viewModel.onEvent(GlobalChatUiEvent.DeleteMessageForEveryone(msg.id))
                            isSheetOpen = false
                        }
                    )
                }

                BottomSheetActionItem(
                    icon = Icons.Default.DeleteOutline,
                    label = "Delete for Me",
                    textColor = MaterialTheme.colorScheme.error,
                    onClick = {
                        viewModel.onEvent(GlobalChatUiEvent.DeleteMessageForMe(msg.id))
                        isSheetOpen = false
                    }
                )

                BottomSheetActionItem(
                    icon = Icons.AutoMirrored.Default.Send,
                    label = "Forward Message",
                    onClick = {
                        Toast.makeText(context, "Select contact to forward message.", Toast.LENGTH_SHORT).show()
                        isSheetOpen = false
                    }
                )

                BottomSheetActionItem(
                    icon = Icons.Default.Share,
                    label = "Share",
                    onClick = {
                        Toast.makeText(context, "Sharing payload prepared.", Toast.LENGTH_SHORT).show()
                        isSheetOpen = false
                    }
                )

                BottomSheetActionItem(
                    icon = Icons.Default.Info,
                    label = "Message Info",
                    onClick = {
                        val status = when (msg.deliveryStatus.uppercase()) {
                            "SENT" -> "Sent"
                            "DELIVERED" -> "Delivered"
                            else -> msg.deliveryStatus
                        }
                        Toast.makeText(context, "Status: $status\nSent at: ${com.mesh.emergency.core.utils.DateUtils.formatMessageTime(context, msg.timestamp)}", Toast.LENGTH_LONG).show()
                        isSheetOpen = false
                    }
                )
            }
        }
    }
}

@Composable
private fun BottomSheetActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = if (textColor == MaterialTheme.colorScheme.onSurface) MaterialTheme.colorScheme.onSurfaceVariant else textColor, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = label, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = textColor)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Message Bubble
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GlobalMessageBubble(
    message: GlobalChatMessage,
    isSelected: Boolean,
    highlightedMessageId: String?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onReplyClick: (String) -> Unit
) {
    val isSelf = message.isSelf
    val context = LocalContext.current

    val highlightColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        } else if (message.id == highlightedMessageId) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        } else {
            Color.Transparent
        },
        animationSpec = tween(durationMillis = 600),
        label = "HighlightFade"
    )

    Row(
        horizontalArrangement = if (isSelf) Arrangement.End else Arrangement.Start,
        modifier = Modifier
            .fillMaxWidth()
            .background(highlightColor)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isSelf) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = message.senderName.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
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
                        if (isSelf) MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                    )
                    .padding(8.dp)
            ) {
                Column {
                    // Nested Reply Preview
                    if (message.replyToMessageId != null) {
                        val isOriginalDeleted = message.replyToContent == null || 
                                message.replyToContent.contains("deleted this message", ignoreCase = true) || 
                                message.replyToContent.contains("deleted this message.", ignoreCase = true)
                        val replyPreviewText = if (isOriginalDeleted) "Original message unavailable." else message.replyToContent

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelf) Color.Black.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                )
                                .clickable { onReplyClick(message.replyToMessageId) }
                                .padding(vertical = 6.dp, horizontal = 10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(32.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(
                                            if (isSelf) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.primary
                                        )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = message.replyToSenderName ?: "Unknown",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isSelf) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = replyPreviewText,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontStyle = if (isOriginalDeleted) FontStyle.Italic else FontStyle.Normal
                                        ),
                                        color = if (isSelf) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    val displayContent = when {
                        message.deleted -> if (isSelf) "You deleted this message." else "${message.senderName} deleted this message."
                        else -> message.content
                    }
                    val textStyle = if (message.deleted) {
                        MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic)
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
                        color = textColor,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
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
                        style = MaterialTheme.typography.labelSmall.copy(fontStyle = FontStyle.Italic),
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
    replyingToMessage: GlobalChatMessage?,
    focusRequester: FocusRequester,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onCancelEdit: () -> Unit,
    onCancelReply: () -> Unit
) {
    val isEditing = editingMessage != null
    val isReplying = replyingToMessage != null

    val sendButtonScale by animateFloatAsState(
        targetValue = if (draft.isNotBlank()) 1f else 0.8f,
        animationSpec = tween(200),
        label = "SendButtonScale"
    )

    Column(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)
    ) {
        // Edit Mode Header Block (Redesigned with vertical color indicator bar)
        AnimatedVisibility(
            visible = isEditing,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            if (isEditing) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(36.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.secondary)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Edit Broadcast",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = editingMessage!!.content,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    IconButton(onClick = onCancelEdit, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Inline Reply Header Block (Redesigned with vertical color indicator bar)
        AnimatedVisibility(
            visible = isReplying,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            if (isReplying) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(36.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = replyingToMessage!!.senderName,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            val isOriginalDeleted = replyingToMessage.content.contains("deleted this message", ignoreCase = true) || replyingToMessage.content.contains("deleted this message.", ignoreCase = true)
                            val textToShow = if (isOriginalDeleted) "Original message unavailable." else replyingToMessage.content
                            Text(
                                text = textToShow,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    IconButton(onClick = onCancelReply, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
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
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
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
                    imageVector = if (isEditing) Icons.Default.Check else Icons.AutoMirrored.Default.Send,
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

// ─────────────────────────────────────────────────────────────────────────────
// Swipe To Reply Box
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SwipeToReplyBox(
    onReply: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    var hasHapticTriggered by remember { mutableStateOf(false) }

    val density = androidx.compose.ui.platform.LocalDensity.current
    val thresholdPx = remember(density) { with(density) { 70.dp.toPx() } }
    val maxDragPx = remember(density) { with(density) { 100.dp.toPx() } }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        val newOffset = (offsetX.value + dragAmount).coerceIn(0f, maxDragPx)
                        scope.launch {
                            offsetX.snapTo(newOffset)
                        }

                        if (newOffset >= thresholdPx && !hasHapticTriggered) {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            hasHapticTriggered = true
                        } else if (newOffset < thresholdPx) {
                            hasHapticTriggered = false
                        }
                    },
                    onDragEnd = {
                        scope.launch {
                            if (offsetX.value >= thresholdPx) {
                                onReply()
                            }
                            offsetX.animateTo(0f)
                            hasHapticTriggered = false
                        }
                    },
                    onDragCancel = {
                        scope.launch {
                            offsetX.animateTo(0f)
                            hasHapticTriggered = false
                        }
                    }
                )
            }
    ) {
        if (offsetX.value > 0f) {
            val progress = (offsetX.value / thresholdPx).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp)
                    .size(36.dp)
                    .scale(progress)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Default.Reply,
                    contentDescription = "Reply",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    translationX = offsetX.value
                }
        ) {
            content()
        }
    }
}
