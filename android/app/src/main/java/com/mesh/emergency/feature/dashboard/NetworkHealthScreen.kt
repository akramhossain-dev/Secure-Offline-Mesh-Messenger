/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.feature.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mesh.emergency.core.designsystem.component.AuroraBackdrop
import com.mesh.emergency.core.designsystem.component.GlassPanel
import com.mesh.emergency.core.designsystem.component.GlassPanelVariant
import com.mesh.emergency.core.designsystem.component.ScanningRipple
import com.mesh.emergency.core.designsystem.theme.MeshThemeTokens
import com.mesh.emergency.domain.repository.NodeDomainModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Network Health Dashboard — real-time mesh connectivity monitoring.
 *
 * Replaces stub NetworkScreen. Connected to [NetworkHealthViewModel]
 * which aggregates live data from [NetworkHealthManager].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkHealthScreen(
    viewModel: NetworkHealthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val spacing = MeshThemeTokens.spacing
    val semanticColors = MeshThemeTokens.semanticColors

    val statusColor = when (uiState.networkStatus) {
        NetworkStatus.CONNECTED    -> semanticColors.connected
        NetworkStatus.DEGRADED     -> semanticColors.warning
        NetworkStatus.NO_CONNECTION -> semanticColors.offline
    }

    AuroraBackdrop(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "Network Health",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                uiState.networkStatus.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColor
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
                    top = paddingValues.calculateTopPadding() + spacing.xs,
                    bottom = spacing.lg
                ),
                verticalArrangement = Arrangement.spacedBy(spacing.md),
                modifier = Modifier.fillMaxSize()
            ) {

                // ── Mesh scanning status ───────────────────────────────────────
                item {
                    GlassPanel(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ScanningRipple(
                                color = statusColor,
                                size = 52.dp
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                uiState.networkStatus.label,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = statusColor
                            )
                            Text(
                                if (uiState.connectedNodes == 0)
                                    "No mesh nodes discovered"
                                else
                                    "${uiState.connectedNodes} node(s) in mesh range",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // ── Stats grid ────────────────────────────────────────────────
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        StatCard(
                            label = "Nodes",
                            value = uiState.connectedNodes.toString(),
                            icon = Icons.Default.Hub,
                            tint = semanticColors.connected,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "Links",
                            value = uiState.activeConnections.toString(),
                            icon = Icons.Default.SignalCellularAlt,
                            tint = semanticColors.info,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "Signal",
                            value = "%.0f%%".format(uiState.signalQuality),
                            icon = Icons.Default.Circle,
                            tint = if (uiState.signalQuality > 50) semanticColors.connected else semanticColors.warning,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // ── Signal quality bar ────────────────────────────────────────
                item {
                    SectionLabel("Signal Quality")
                    GlassPanel(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Average RSSI Quality", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("%.1f%%".format(uiState.signalQuality), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                            }
                            val animatedProgress by animateFloatAsState(
                                targetValue = uiState.signalQuality / 100f,
                                animationSpec = tween(600),
                                label = "signalQuality"
                            )
                            LinearProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier.fillMaxWidth(),
                                color = when {
                                    uiState.signalQuality > 70 -> semanticColors.connected
                                    uiState.signalQuality > 40 -> semanticColors.warning
                                    else                       -> semanticColors.emergency
                                },
                                strokeCap = StrokeCap.Round
                            )
                        }
                    }
                }

                // ── Failure rate ──────────────────────────────────────────────
                item {
                    SectionLabel("Reliability")
                    GlassPanel(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Failure Rate", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("%.1f%%".format(uiState.failureRate * 100), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = if (uiState.failureRate > 0.3f) semanticColors.warning else semanticColors.connected)
                            }
                            val animatedFailure by animateFloatAsState(
                                targetValue = uiState.failureRate,
                                animationSpec = tween(600),
                                label = "failureRate"
                            )
                            LinearProgressIndicator(
                                progress = { animatedFailure },
                                modifier = Modifier.fillMaxWidth(),
                                color = if (uiState.failureRate > 0.3f) semanticColors.warning else semanticColors.connected,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                strokeCap = StrokeCap.Round
                            )
                        }
                    }
                }

                // ── Connection type ───────────────────────────────────────────
                item {
                    SectionLabel("Connection")
                    InfoRow("Type", uiState.connectionType)
                    InfoRow("Last Activity",
                        if (uiState.lastActivityTime == 0L) "None"
                        else SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(uiState.lastActivityTime))
                    )
                }

                // ── Offline mode panel ────────────────────────────────────────
                if (uiState.networkStatus == NetworkStatus.NO_CONNECTION) {
                    item {
                        GlassPanel(modifier = Modifier.fillMaxWidth(), variant = GlassPanelVariant.WARNING) {
                            Column {
                                Text(
                                    "Offline Mode Active",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = semanticColors.warning
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Messages are queued locally. They will be delivered once a mesh connection is established.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // ── Recent nodes ──────────────────────────────────────────────
                if (uiState.recentNodes.isNotEmpty()) {
                    item { SectionLabel("Recent Nodes") }
                    items(uiState.recentNodes, key = { it.id }) { node ->
                        RecentNodeRow(node = node)
                    }
                }

                // ── Simulation controls (debug) ────────────────────────────────
                item {
                    SectionLabel("Simulation (Debug)")
                    GlassPanel(modifier = Modifier.fillMaxWidth()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = viewModel::onSimulateNodeJoin, modifier = Modifier.weight(1f)) {
                                Text("+ Node", style = MaterialTheme.typography.labelSmall)
                            }
                            Button(onClick = viewModel::onSimulateNodeLeave, modifier = Modifier.weight(1f)) {
                                Text("- Node", style = MaterialTheme.typography.labelSmall)
                            }
                            Button(onClick = viewModel::onSimulateFailure, modifier = Modifier.weight(1f)) {
                                Text("Fail", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    GlassPanel(modifier = modifier, contentPadding = 12.dp) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SectionLabel(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    GlassPanel(modifier = Modifier.fillMaxWidth(), contentPadding = 10.dp) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.onSurface)
        }
    }
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun RecentNodeRow(node: NodeDomainModel) {
    GlassPanel(modifier = Modifier.fillMaxWidth(), contentPadding = 10.dp) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Default.Circle,
                contentDescription = null,
                tint = when (node.status) {
                    "ONLINE" -> MeshThemeTokens.semanticColors.connected
                    "WEAK_CONNECTION" -> MeshThemeTokens.semanticColors.warning
                    else -> MeshThemeTokens.semanticColors.offline
                },
                modifier = Modifier.size(8.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(node.deviceId.take(16), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.onSurface)
                Text("${node.type} • RSSI: ${node.rssi} dBm", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(node.status, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
