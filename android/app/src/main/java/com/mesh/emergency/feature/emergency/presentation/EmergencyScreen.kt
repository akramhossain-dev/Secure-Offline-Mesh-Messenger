/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.feature.emergency.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mesh.emergency.R
import com.mesh.emergency.core.designsystem.component.AlertPriorityLevel
import com.mesh.emergency.core.designsystem.component.AuroraBackdrop
import com.mesh.emergency.core.designsystem.component.GlassPanel
import com.mesh.emergency.core.designsystem.component.GlassPanelVariant
import com.mesh.emergency.core.designsystem.component.MeshButton
import com.mesh.emergency.core.designsystem.component.MeshOutlinedButton
import com.mesh.emergency.core.designsystem.component.MeshSosButton
import com.mesh.emergency.core.designsystem.component.PriorityBadge
import com.mesh.emergency.core.designsystem.component.PulsingRing
import com.mesh.emergency.core.designsystem.theme.MeshThemeTokens
import com.mesh.emergency.data.local.entity.DbEmergencyStatus
import com.mesh.emergency.data.local.entity.DbEmergencyType
import com.mesh.emergency.data.local.entity.DbMessagePriority
import com.mesh.emergency.feature.emergency.domain.EmergencyEvent
import com.mesh.emergency.feature.emergency.domain.SosState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Emergency Dashboard screen.
 *
 * Shows the SOS action panel, active emergency events with priority
 * visualization, and a history section. Follows the high-visibility Aurora
 * design direction with emergency glass tints.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyScreen(
    onBack: () -> Unit = {},
    viewModel: EmergencyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val spacing = MeshThemeTokens.spacing
    val semanticColors = MeshThemeTokens.semanticColors

    var showSosConfirmDialog by remember { mutableStateOf(false) }

    // Collect one-shot effects
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                EmergencyUiEffect.SosConfirmationRequired -> showSosConfirmDialog = true
                EmergencyUiEffect.SosActivated           -> snackbarHostState.showSnackbar("🚨 SOS Activated — broadcasting to mesh network")
                EmergencyUiEffect.SosCancelled           -> snackbarHostState.showSnackbar("SOS cancelled")
                is EmergencyUiEffect.ShowMessage         -> snackbarHostState.showSnackbar(effect.text)
            }
        }
    }

    // SOS Confirmation Dialog
    if (showSosConfirmDialog) {
        SosConfirmationDialog(
            onConfirm = {
                showSosConfirmDialog = false
                viewModel.onEvent(EmergencyUiEvent.ConfirmSos)
            },
            onDismiss = {
                showSosConfirmDialog = false
                viewModel.onEvent(EmergencyUiEvent.CancelSos)
            }
        )
    }

    AuroraBackdrop(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (uiState.sosState == SosState.ACTIVE) {
                                PulsingRing(color = semanticColors.emergency, size = 16.dp)
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(
                                text = stringResource(R.string.emergency_title),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Default.ArrowBack,
                                contentDescription = stringResource(R.string.action_back)
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
                    bottom = paddingValues.calculateBottomPadding() + spacing.lg
                ),
                verticalArrangement = Arrangement.spacedBy(spacing.md),
                modifier = Modifier.fillMaxSize()
            ) {

                // ── SOS Status Banner (only when active) ──────────────────────
                item {
                    AnimatedVisibility(
                        visible = uiState.sosState == SosState.ACTIVE,
                        enter = fadeIn(tween(300)) + slideInVertically(),
                        exit = fadeOut(tween(300))
                    ) {
                        GlassPanel(
                            modifier = Modifier.fillMaxWidth(),
                            variant = GlassPanelVariant.EMERGENCY
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "SOS Active",
                                    tint = semanticColors.emergency,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(spacing.sm))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "SOS ACTIVE",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                        color = semanticColors.emergency
                                    )
                                    Text(
                                        "Distress signal broadcasting to all reachable mesh nodes",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(Modifier.width(spacing.sm))
                                TextButton(onClick = { viewModel.onEvent(EmergencyUiEvent.ResolveEvent(
                                    uiState.activeEvents.firstOrNull()?.id ?: return@TextButton
                                )) }) {
                                    Text("Resolve", color = semanticColors.success)
                                }
                            }
                        }
                    }
                }

                // ── SOS Action Panel ──────────────────────────────────────────
                item {
                    SosActionPanel(
                        sosState = uiState.sosState,
                        onSosPressed = { viewModel.onEvent(EmergencyUiEvent.InitiateSos) }
                    )
                }

                // ── Active Events ─────────────────────────────────────────────
                if (uiState.activeEvents.isNotEmpty()) {
                    item {
                        SectionHeader(
                            icon = { Icon(Icons.Default.Warning, null, tint = semanticColors.emergency, modifier = Modifier.size(16.dp)) },
                            title = stringResource(R.string.sos_active_events_count, uiState.activeEvents.size)
                        )
                    }
                    items(uiState.activeEvents, key = { it.id }) { event ->
                        EmergencyEventCard(
                            event = event,
                            onAcknowledge = { viewModel.onEvent(EmergencyUiEvent.AcknowledgeEvent(event.id)) },
                            onResolve     = { viewModel.onEvent(EmergencyUiEvent.ResolveEvent(event.id)) }
                        )
                    }
                }

                // ── History ───────────────────────────────────────────────────
                item {
                    SectionHeader(
                        icon = { Icon(Icons.Default.History, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp)) },
                        title = stringResource(R.string.sos_history_count, uiState.historyEvents.size)
                    )
                }

                if (uiState.historyEvents.isEmpty()) {
                    item {
                        GlassPanel(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                stringResource(R.string.sos_no_history),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(uiState.historyEvents, key = { "${it.id}-history" }) { event ->
                        EmergencyHistoryCard(event = event)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SOS Action Panel
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SosActionPanel(sosState: SosState, onSosPressed: () -> Unit) {
    val semanticColors = MeshThemeTokens.semanticColors
    val spacing = MeshThemeTokens.spacing

    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        variant = if (sosState == SosState.ACTIVE) GlassPanelVariant.EMERGENCY else GlassPanelVariant.DEFAULT
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // State label
            Text(
                text = when (sosState) {
                    SosState.READY        -> "Hold for Emergency SOS"
                    SosState.CONFIRMING   -> "Confirm SOS?"
                    SosState.ACTIVE       -> "🚨 SOS ACTIVE"
                    SosState.ACKNOWLEDGED -> "✓ Acknowledged"
                    SosState.RESOLVED     -> "✓ Resolved"
                },
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = when (sosState) {
                    SosState.ACTIVE -> semanticColors.emergency
                    SosState.RESOLVED, SosState.ACKNOWLEDGED -> semanticColors.success
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = when (sosState) {
                    SosState.READY   -> "Broadcasts distress signal to all nearby nodes"
                    SosState.ACTIVE  -> "Signal queued for mesh delivery"
                    else             -> "No active emergency"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(spacing.lg))

            // Only show button when not active/resolved
            if (sosState == SosState.READY || sosState == SosState.CONFIRMING) {
                MeshSosButton(onSosTriggered = onSosPressed)
            }

            if (sosState == SosState.ACTIVE) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "SOS Active",
                    tint = semanticColors.emergency,
                    modifier = Modifier.size(48.dp)
                )
            }

            if (sosState == SosState.RESOLVED || sosState == SosState.ACKNOWLEDGED) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Resolved",
                    tint = semanticColors.success,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Emergency Event Card (active)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EmergencyEventCard(
    event: EmergencyEvent,
    onAcknowledge: () -> Unit,
    onResolve: () -> Unit
) {
    val semanticColors = MeshThemeTokens.semanticColors
    val spacing = MeshThemeTokens.spacing

    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        variant = GlassPanelVariant.EMERGENCY,
        contentPadding = 14.dp
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = event.type.name.replace("_", " "),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = semanticColors.emergency
                    )
                    Text(
                        text = "From: ${event.senderId}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                PriorityBadge(level = event.priority.toUiLevel())
            }

            Spacer(Modifier.height(spacing.sm))
            Text(
                text = event.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = formatTimestamp(event.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(spacing.md))

            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                MeshOutlinedButton(
                    text = stringResource(R.string.sos_acknowledge),
                    onClick = onAcknowledge,
                    modifier = Modifier.weight(1f)
                )
                MeshButton(
                    text = stringResource(R.string.sos_resolve),
                    onClick = onResolve,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Emergency History Card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EmergencyHistoryCard(event: EmergencyEvent) {
    val semanticColors = MeshThemeTokens.semanticColors

    GlassPanel(modifier = Modifier.fillMaxWidth(), contentPadding = 12.dp) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Resolved",
                tint = semanticColors.success,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.type.name,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = formatTimestamp(event.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StatusChip(status = event.status)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SOS Confirmation Dialog
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SosConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Warning,
                contentDescription = "Warning",
                tint = MeshThemeTokens.semanticColors.emergency,
                modifier = Modifier.size(28.dp)
            )
        },
        title = { Text("Send SOS?", fontWeight = FontWeight.Bold) },
        text = {
            Text(
                "This will broadcast an emergency distress signal to all reachable mesh nodes. " +
                "Only use in genuine emergencies.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            MeshButton(
                text = "🚨 Send SOS",
                onClick = onConfirm
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Helper composables
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(icon: @Composable () -> Unit, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        icon()
        Spacer(Modifier.width(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun StatusChip(status: DbEmergencyStatus) {
    val semanticColors = MeshThemeTokens.semanticColors
    val (label, color) = when (status) {
        DbEmergencyStatus.RESOLVED     -> "Resolved"     to semanticColors.success
        DbEmergencyStatus.ACKNOWLEDGED -> "Acknowledged" to semanticColors.info
        DbEmergencyStatus.CANCELLED    -> "Cancelled"    to semanticColors.offline
        DbEmergencyStatus.RECEIVED     -> "Received"     to semanticColors.connected
        DbEmergencyStatus.BROADCASTING -> "Broadcasting" to semanticColors.emergency
        else                           -> status.name    to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
        color = color
    )
}

private fun DbMessagePriority.toUiLevel() = when (this) {
    DbMessagePriority.CRITICAL -> AlertPriorityLevel.CRITICAL
    DbMessagePriority.HIGH     -> AlertPriorityLevel.HIGH
    DbMessagePriority.MEDIUM   -> AlertPriorityLevel.NORMAL
    DbMessagePriority.LOW      -> AlertPriorityLevel.LOW
}

private fun formatTimestamp(ts: Long): String =
    SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(ts))
