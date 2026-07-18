/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.mesh.emergency.core.designsystem.component.AuroraBackdrop
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.mesh.emergency.core.designsystem.component.GlassPanel
import com.mesh.emergency.core.designsystem.component.GlassPanelVariant
import com.mesh.emergency.core.designsystem.component.MeshConnectionStatus
import com.mesh.emergency.core.designsystem.component.MeshSignalIndicator
import com.mesh.emergency.core.designsystem.component.ScanningRipple
import com.mesh.emergency.core.designsystem.theme.MeshThemeTokens

/**
 * Network Status Screen — shows transport, node count, signal quality,
 * and offline mode information.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkScreen(
    viewModel: NetworkViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val spacing = MeshThemeTokens.spacing
    val semanticColors = MeshThemeTokens.semanticColors

    val isConnected = uiState.isConnected
    val nodeCount = uiState.nodeCount
    val transport = uiState.transport
    val rssi = uiState.rssi

    AuroraBackdrop(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Network", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
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

                // ── Mesh scanning animation ───────────────────────────────────
                item {
                    GlassPanel(modifier = Modifier.fillMaxWidth(), variant = GlassPanelVariant.DEFAULT) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            ScanningRipple(
                                color = if (isConnected) semanticColors.connected else semanticColors.offline,
                                size = 48.dp
                            )
                            Spacer(Modifier.height(spacing.md))
                            Text(
                                text = if (isConnected) "Mesh Connected" else "No Mesh Connection",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (isConnected) semanticColors.connected else semanticColors.offline
                            )
                            Text(
                                text = if (isConnected) "$nodeCount node(s) in range" else "Start Bluetooth or configure LoRa module",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // ── Transport status ──────────────────────────────────────────
                item { SectionLabel("Transport") }
                item {
                    NetworkInfoRow(label = "Active Transport",   value = transport.ifBlank { "None" })
                    NetworkInfoRow(
                        label = "Bluetooth",
                        value = when {
                            uiState.isBluetoothConnected -> "Connected"
                            uiState.isBluetoothEnabled -> "Enabled (Available)"
                            else -> "Disabled"
                        }
                    )
                    NetworkInfoRow(
                        label = "LoRa",
                        value = when {
                            uiState.isLoRaConnected -> "Connected"
                            uiState.isLoRaEnabled -> "Enabled (Available)"
                            else -> "Disabled"
                        }
                    )
                }

                // ── Signal quality ────────────────────────────────────────────
                item { SectionLabel("Signal Quality") }
                item {
                    GlassPanel(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("RSSI", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            MeshSignalIndicator(rssi = rssi)
                        }
                    }
                }

                // ── Connection info ───────────────────────────────────────────
                item { SectionLabel("Status") }
                item {
                    GlassPanel(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Connection", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            MeshConnectionStatus(isConnected = isConnected)
                        }
                    }
                }

                // ── Offline mode note ─────────────────────────────────────────
                item {
                    GlassPanel(modifier = Modifier.fillMaxWidth(), variant = GlassPanelVariant.WARNING) {
                        Column {
                            Text(
                                "Offline Mode Active",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = semanticColors.warning
                            )
                            Spacer(Modifier.height(spacing.xs))
                            Text(
                                "All messages are queued locally and will be delivered when a mesh connection is established.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun NetworkInfoRow(label: String, value: String) {
    GlassPanel(modifier = Modifier.fillMaxWidth(), contentPadding = 10.dp) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.onSurface)
        }
    }
    Spacer(Modifier.height(MeshThemeTokens.spacing.xs))
}


