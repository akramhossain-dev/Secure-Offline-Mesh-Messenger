/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.feature.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mesh.emergency.core.designsystem.component.AlertPriorityLevel
import com.mesh.emergency.core.designsystem.component.AuroraBackdrop
import com.mesh.emergency.core.designsystem.component.GlassPanel
import com.mesh.emergency.core.designsystem.component.GlassPanelVariant
import com.mesh.emergency.core.designsystem.component.MeshConnectionStatus
import com.mesh.emergency.core.designsystem.component.PriorityBadge
import com.mesh.emergency.core.designsystem.theme.MeshThemeTokens

/**
 * Communication Overview Screen — displays message queue summary,
 * delivery status, and transport state. No real messaging.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunicationScreen() {
    val spacing = MeshThemeTokens.spacing
    val semanticColors = MeshThemeTokens.semanticColors

    AuroraBackdrop(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Communication",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Offline Queue",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { paddingValues ->
            LazyColumn(
                contentPadding = PaddingValues(
                    start = spacing.lg, end = spacing.lg,
                    top = paddingValues.calculateTopPadding() + spacing.sm,
                    bottom = spacing.lg
                ),
                verticalArrangement = Arrangement.spacedBy(spacing.md),
                modifier = Modifier.fillMaxSize()
            ) {

                // ── Queue summary ─────────────────────────────────────────────
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                        QueueSummaryCard(
                            modifier = Modifier.weight(1f),
                            label = "Queued",
                            count = 3,
                            color = semanticColors.warning
                        )
                        QueueSummaryCard(
                            modifier = Modifier.weight(1f),
                            label = "Delivered",
                            count = 12,
                            color = semanticColors.success
                        )
                        QueueSummaryCard(
                            modifier = Modifier.weight(1f),
                            label = "Failed",
                            count = 1,
                            color = semanticColors.emergency
                        )
                    }
                }

                // ── Transport status ──────────────────────────────────────────
                item {
                    GlassPanel(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = "Active Transport",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                                )
                                Text(
                                    text = "None — configure in Phase A28",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            MeshConnectionStatus(isConnected = false)
                        }
                    }
                }

                // ── Pending messages ──────────────────────────────────────────
                item {
                    Text(
                        text = "Pending Messages",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                item {
                    PendingMessageRow(
                        recipientId = "MESH-NODE-001",
                        preview = "Emergency supply request — Water 50L",
                        retryCount = 2,
                        priority = AlertPriorityLevel.HIGH
                    )
                }
                item {
                    PendingMessageRow(
                        recipientId = "MESH-NODE-002",
                        preview = "Field status update message",
                        retryCount = 0,
                        priority = AlertPriorityLevel.NORMAL
                    )
                }
                item {
                    PendingMessageRow(
                        recipientId = "BROADCAST",
                        preview = "SOS distress beacon data packet",
                        retryCount = 4,
                        priority = AlertPriorityLevel.CRITICAL
                    )
                }

                // ── Offline notice ────────────────────────────────────────────
                item {
                    GlassPanel(modifier = Modifier.fillMaxWidth(), variant = GlassPanelVariant.WARNING) {
                        Text(
                            text = "Messages are stored locally and will transmit automatically when a mesh node is reachable.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueSummaryCard(
    modifier: Modifier,
    label: String,
    count: Int,
    color: androidx.compose.ui.graphics.Color
) {
    GlassPanel(modifier = modifier, contentPadding = 12.dp) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PendingMessageRow(
    recipientId: String,
    preview: String,
    retryCount: Int,
    priority: AlertPriorityLevel
) {
    GlassPanel(modifier = Modifier.fillMaxWidth(), contentPadding = 12.dp) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "→ $recipientId",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MeshThemeTokens.semanticColors.info
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = preview,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2
                )
                if (retryCount > 0) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Retried $retryCount×",
                        style = MaterialTheme.typography.labelSmall,
                        color = MeshThemeTokens.semanticColors.warning
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            PriorityBadge(level = priority)
        }
    }
}
