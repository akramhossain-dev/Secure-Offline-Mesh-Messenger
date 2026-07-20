/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.feature.message.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mesh.emergency.core.designsystem.component.*
import com.mesh.emergency.core.designsystem.icon.MeshIcons
import com.mesh.emergency.core.designsystem.theme.MeshThemeTokens
import com.mesh.emergency.data.local.entity.DbDeliveryStatus
import com.mesh.emergency.data.local.entity.DbMessagePriority
import com.mesh.emergency.feature.message.domain.ConversationSummary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Conversation / Message list screen — shows all local offline conversations
 * ordered by last activity. Tapping opens the chat screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageListScreen(
    onOpenConversation: (id: String, label: String) -> Unit,
    viewModel: MessageListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val spacing = MeshThemeTokens.spacing

    // No-op: Navigation handled directly via row click handler

    AuroraBackdrop(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Messages",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Offline queue active",
                                style = MaterialTheme.typography.labelSmall,
                                color = MeshThemeTokens.semanticColors.warning
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { paddingValues ->
            if (uiState.isLoading) {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = spacing.lg, end = spacing.lg,
                        top = paddingValues.calculateTopPadding() + spacing.sm,
                        bottom = spacing.lg
                    ),
                    verticalArrangement = Arrangement.spacedBy(spacing.md),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(4) {
                        ShimmerPlaceholder(modifier = Modifier.fillMaxWidth().height(72.dp))
                    }
                }
            } else if (uiState.conversations.isEmpty()) {
                MeshEmptyState(
                    title = "No Conversations Yet",
                    description = "Pair with nearby contacts using QR codes to start chat conversations.",
                    icon = MeshIcons.Chat,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = spacing.lg, end = spacing.lg,
                        top = paddingValues.calculateTopPadding() + spacing.sm,
                        bottom = spacing.lg
                    ),
                    verticalArrangement = Arrangement.spacedBy(spacing.sm),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.conversations, key = { it.id }) { conv ->
                        ConversationRow(
                            conversation = conv,
                            onClick = {
                                onOpenConversation(conv.id, conv.title)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationRow(conversation: ConversationSummary, onClick: () -> Unit) {
    val spacing = MeshThemeTokens.spacing

    GlassPanel(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        contentPadding = 12.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Avatar circle placeholder
            Icon(
                imageVector = Icons.AutoMirrored.Default.Chat,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
            Spacer(Modifier.width(spacing.md))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = conversation.title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = formatShortTime(conversation.updatedAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(2.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = conversation.lastMessagePreview,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    if (conversation.unreadCount > 0) {
                        UnreadBadge(count = conversation.unreadCount)
                    }
                }
            }
        }
    }
}

@Composable
private fun UnreadBadge(count: Int, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(20.dp)
            .background(
                color = MaterialTheme.colorScheme.primary,
                shape = androidx.compose.foundation.shape.CircleShape
            )
    ) {
        Text(
            text = if (count > 9) "9+" else count.toString(),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

private fun formatShortTime(ts: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ts))

fun deliveryStatusLabel(status: DbDeliveryStatus): String = when (status) {
    DbDeliveryStatus.PENDING   -> "⏳"
    DbDeliveryStatus.QUEUED    -> "📦"
    DbDeliveryStatus.SENDING   -> "↑"
    DbDeliveryStatus.SENT      -> "✓"
    DbDeliveryStatus.DELIVERED -> "✓✓"
    DbDeliveryStatus.FAILED    -> "✗"
    DbDeliveryStatus.EXPIRED   -> "⌛"
}

@Composable
fun deliveryStatusColor(status: DbDeliveryStatus): androidx.compose.ui.graphics.Color =
    when (status) {
        DbDeliveryStatus.DELIVERED -> MeshThemeTokens.semanticColors.success
        DbDeliveryStatus.FAILED, DbDeliveryStatus.EXPIRED -> MeshThemeTokens.semanticColors.emergency
        DbDeliveryStatus.QUEUED, DbDeliveryStatus.PENDING -> MeshThemeTokens.semanticColors.warning
        else -> MeshThemeTokens.semanticColors.info
    }

@Composable
fun priorityColor(priority: DbMessagePriority): androidx.compose.ui.graphics.Color =
    when (priority) {
        DbMessagePriority.CRITICAL -> MeshThemeTokens.semanticColors.emergency
        DbMessagePriority.HIGH     -> MeshThemeTokens.semanticColors.warning
        else                       -> MeshThemeTokens.semanticColors.info
    }
