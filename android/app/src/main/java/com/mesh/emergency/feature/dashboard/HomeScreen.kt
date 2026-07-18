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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mesh.emergency.core.designsystem.component.AuroraBackdrop
import com.mesh.emergency.core.designsystem.component.BatteryStatusCard
import com.mesh.emergency.core.designsystem.component.GlassPanel
import com.mesh.emergency.core.designsystem.component.GlassPanelVariant
import com.mesh.emergency.core.designsystem.component.MeshSosButton
import com.mesh.emergency.core.designsystem.component.NetworkStatusCard
import com.mesh.emergency.core.designsystem.component.SosIndicatorBanner
import com.mesh.emergency.core.designsystem.theme.MeshThemeTokens

/**
 * Home Dashboard — the primary landing screen.
 *
 * Shows: SOS banner (if active), network status, battery status,
 * recent activity log, and the SOS button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToEmergency: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val spacing = MeshThemeTokens.spacing

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                HomeUiEffect.NavigateToEmergency -> onNavigateToEmergency()
                is HomeUiEffect.ShowToast -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    AuroraBackdrop(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = uiState.appName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = if (uiState.isOnline) "Mesh Connected" else "Offline Mode",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (uiState.isOnline)
                                    MeshThemeTokens.semanticColors.connected
                                else
                                    MeshThemeTokens.semanticColors.offline
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.onEvent(HomeUiEvent.RefreshStatus) }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { paddingValues ->
            LazyColumn(
                contentPadding = PaddingValues(
                    start = spacing.lg,
                    end = spacing.lg,
                    top = paddingValues.calculateTopPadding() + spacing.sm,
                    bottom = paddingValues.calculateBottomPadding() + spacing.lg
                ),
                verticalArrangement = Arrangement.spacedBy(spacing.md),
                modifier = Modifier.fillMaxSize()
            ) {

                // ── SOS Active Banner ─────────────────────────────────────────
                if (uiState.activeSos) {
                    item {
                        SosIndicatorBanner(
                            isActive = true,
                            messageText = "Emergency SOS is broadcasting to all mesh nodes",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // ── Network Status ────────────────────────────────────────────
                item {
                    NetworkStatusCard(
                        transportLabel = uiState.activeTransport.ifBlank { "No Transport" },
                        isConnected = uiState.isOnline,
                        nodeCount = uiState.connectedNodeCount,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // ── Battery Status ────────────────────────────────────────────
                item {
                    BatteryStatusCard(
                        batteryLevel = uiState.batteryLevel,
                        isCharging = uiState.isCharging,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // ── SOS Button ────────────────────────────────────────────────
                item {
                    GlassPanel(
                        modifier = Modifier.fillMaxWidth(),
                        variant = GlassPanelVariant.DEFAULT
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Emergency SOS",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Press and hold to broadcast distress signal",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(16.dp))
                            MeshSosButton(
                                onSosTriggered = { viewModel.onEvent(HomeUiEvent.SosButtonPressed) }
                            )
                        }
                    }
                }

                // ── Recent Activity ───────────────────────────────────────────
                item {
                    Text(
                        text = "Recent Activity",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                items(uiState.recentActivity, key = { it.id }) { item ->
                    ActivityRow(item = item)
                }

                if (uiState.recentActivity.isEmpty()) {
                    item {
                        GlassPanel(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "No recent activity",
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
private fun ActivityRow(item: ActivityItem) {
    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = 12.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            ActivityTypeIndicator(type = item.type)
        }
    }
}

@Composable
private fun ActivityTypeIndicator(type: ActivityType) {
    val semantic = MeshThemeTokens.semanticColors
    val (color, label) = when (type) {
        ActivityType.SOS         -> semantic.emergency to "SOS"
        ActivityType.MESSAGE     -> semantic.info to "MSG"
        ActivityType.NODE_JOINED -> semantic.connected to "NODE"
        ActivityType.RESOURCE    -> semantic.warning to "RES"
        ActivityType.SYSTEM      -> semantic.offline to "SYS"
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
        color = color
    )
}
