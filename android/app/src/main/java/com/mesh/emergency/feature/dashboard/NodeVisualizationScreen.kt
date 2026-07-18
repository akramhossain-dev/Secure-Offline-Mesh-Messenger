/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.feature.dashboard

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DevicesOther
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mesh.emergency.core.designsystem.component.AuroraBackdrop
import com.mesh.emergency.core.designsystem.component.GlassPanel
import com.mesh.emergency.core.designsystem.theme.MeshThemeTokens
import com.mesh.emergency.domain.repository.NodeDomainModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Node Visualization Screen — displays known mesh devices with status indicators.
 *
 * No real mesh routing is implemented — nodes are stored and viewed locally.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NodeVisualizationScreen(
    viewModel: NodeVisualizationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val spacing = MeshThemeTokens.spacing

    AuroraBackdrop(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "Mesh Nodes",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                "${uiState.filteredNodes.size} / ${uiState.allNodes.size} devices",
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
                    top = paddingValues.calculateTopPadding() + spacing.xs,
                    bottom = spacing.lg
                ),
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
                modifier = Modifier.fillMaxSize()
            ) {

                // ── Filter chips ──────────────────────────────────────────────
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        items(NodeFilter.entries, key = { it.name }) { filter ->
                            FilterChip(
                                selected = uiState.selectedFilter == filter,
                                onClick = { viewModel.onFilterSelected(filter) },
                                label = { Text(filter.label, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                    }
                }

                // ── Node count summary ─────────────────────────────────────────
                item {
                    val onlineCount = uiState.allNodes.count { it.status == "ONLINE" }
                    val offlineCount = uiState.allNodes.count { it.status != "ONLINE" }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        NodeSummaryChip("Online", onlineCount, MeshThemeTokens.semanticColors.connected, Modifier.weight(1f))
                        NodeSummaryChip("Offline", offlineCount, MeshThemeTokens.semanticColors.offline, Modifier.weight(1f))
                        NodeSummaryChip("Total", uiState.allNodes.size, MeshThemeTokens.semanticColors.info, Modifier.weight(1f))
                    }
                }

                // ── Node list ─────────────────────────────────────────────────
                if (uiState.filteredNodes.isEmpty()) {
                    item {
                        GlassPanel(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.Default.Hub,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    if (uiState.allNodes.isEmpty())
                                        "No mesh nodes discovered yet.\nNodes appear once mesh contact is made."
                                    else
                                        "No nodes match this filter.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                items(uiState.filteredNodes, key = { it.id }) { node ->
                    NodeCard(
                        node = node,
                        onClick = { viewModel.onNodeSelected(node) }
                    )
                }
            }
        }

        // ── Node detail sheet ──────────────────────────────────────────────────
        uiState.selectedNode?.let { node ->
            NodeDetailSheet(
                node = node,
                onDismiss = viewModel::onNodeDismissed
            )
        }
    }
}

@Composable
private fun NodeSummaryChip(label: String, count: Int, color: Color, modifier: Modifier = Modifier) {
    GlassPanel(modifier = modifier, contentPadding = 10.dp) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(count.toString(), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun NodeCard(node: NodeDomainModel, onClick: () -> Unit) {
    val semanticColors = MeshThemeTokens.semanticColors
    val statusColor = when (node.status) {
        "ONLINE"          -> semanticColors.connected
        "WEAK_CONNECTION" -> semanticColors.warning
        "OFFLINE"         -> semanticColors.offline
        else              -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val typeIcon: ImageVector = when (node.type) {
        "PHONE_NODE"   -> Icons.Default.Smartphone
        "RELAY_NODE"   -> Icons.Default.Router
        "GATEWAY_NODE" -> Icons.Default.Hub
        else           -> Icons.Default.DevicesOther
    }
    val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        contentPadding = 12.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Node type icon with status dot
            Box {
                Icon(
                    typeIcon,
                    contentDescription = node.type,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(36.dp)
                )
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                        .align(Alignment.BottomEnd)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    node.deviceId.take(16),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "${node.type.replace("_", " ")} • RSSI: ${node.rssi} dBm",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Last seen: ${dateFormat.format(Date(node.lastSeen))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Icon(
                    Icons.Default.BatteryStd,
                    contentDescription = "Battery",
                    tint = when {
                        node.batteryLevel > 50 -> semanticColors.connected
                        node.batteryLevel > 20 -> semanticColors.warning
                        else                   -> semanticColors.emergency
                    },
                    modifier = Modifier.size(16.dp)
                )
                Text("${node.batteryLevel}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NodeDetailSheet(node: NodeDomainModel, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartialExpansion = true)
    val spacing = MeshThemeTokens.spacing
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault())

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.lg, vertical = spacing.md)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Node Details", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Close") }
            }
            Spacer(Modifier.height(spacing.md))
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    NodeDetailRow("Device ID", node.deviceId)
                    NodeDetailRow("Type", node.type.replace("_", " "))
                    NodeDetailRow("Status", node.status)
                    NodeDetailRow("RSSI", "${node.rssi} dBm")
                    NodeDetailRow("Signal Quality", "%.1f%%".format(node.rssi.toFloat() + 100))
                    NodeDetailRow("Battery", "${node.batteryLevel}%")
                    NodeDetailRow("Hop Count", node.hopCount.toString())
                    NodeDetailRow("Relay Capable", if (node.relayCapability) "Yes" else "No")
                    NodeDetailRow("Network Distance", "${node.networkDistance} hops")
                    NodeDetailRow("Last Seen", dateFormat.format(Date(node.lastSeen)))
                    if (node.latitude != 0.0 || node.longitude != 0.0) {
                        NodeDetailRow("Location", "%.4f, %.4f".format(node.latitude, node.longitude))
                    }
                }
            }
            Spacer(Modifier.height(spacing.lg))
        }
    }
}

@Composable
private fun NodeDetailRow(label: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.onSurface)
    }
}
