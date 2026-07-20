/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.feature.contacts

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
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mesh.emergency.R
import com.mesh.emergency.core.designsystem.component.*
import com.mesh.emergency.core.designsystem.theme.MeshThemeTokens

/**
 * Device screen showing paired nodes, signal strength, last seen, and
 * a QR pair FAB. No real BLE scanning — uses stub data.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceScreen(
    onNavigateToQrPair: () -> Unit,
    onNavigateToChat: (deviceId: String, deviceName: String) -> Unit = { _, _ -> },
    viewModel: DeviceViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val spacing = MeshThemeTokens.spacing

    var deviceToUnpair by remember { androidx.compose.runtime.mutableStateOf<DeviceDisplayModel?>(null) }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is DeviceUiEffect.ShowToast     -> snackbarHostState.showSnackbar(effect.message)
                DeviceUiEffect.NavigateToQrPair -> onNavigateToQrPair()
                is DeviceUiEffect.NavigateToChat -> onNavigateToChat(effect.deviceId, effect.deviceName)
            }
        }
    }

    if (deviceToUnpair != null) {
        MeshConfirmationDialog(
            title = "Unpair Device",
            message = "Are you sure you want to unpair from '${deviceToUnpair?.name}'? You will not be able to send or receive messages until you pair again.",
            confirmText = "Unpair",
            cancelText = "Cancel",
            onConfirm = {
                viewModel.onEvent(DeviceUiEvent.UnpairDevice(deviceToUnpair!!.id))
                deviceToUnpair = null
            },
            onCancel = {
                deviceToUnpair = null
            }
        )
    }

    if (uiState.isScanning) {
        MeshScanningDialog(
            discoveredCount = uiState.pairedDevices.size,
            onCancel = { viewModel.onEvent(DeviceUiEvent.CancelScan) }
        )
    }

    AuroraBackdrop(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { MeshSnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.contacts_title), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                    actions = {
                        androidx.compose.material3.IconButton(onClick = { viewModel.onEvent(DeviceUiEvent.StartScan) }) {
                            Icon(Icons.Default.Search, contentDescription = stringResource(R.string.contacts_scan))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    text = { Text("QR Pair") },
                    icon = { Icon(Icons.Default.QrCode, contentDescription = "Pair via QR") },
                    onClick = onNavigateToQrPair,
                    containerColor = MaterialTheme.colorScheme.primary
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
                // ── Section header ────────────────────────────────────────────
                item {
                    Text(
                        stringResource(R.string.contacts_paired_count, uiState.pairedDevices.size),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // ── Device rows ───────────────────────────────────────────────
                items(uiState.pairedDevices, key = { it.id }) { device ->
                    DeviceRow(
                        device     = device,
                        onUnpair   = { deviceToUnpair = device },
                        onOpenChat = { viewModel.onEvent(DeviceUiEvent.OpenChat(device.id, device.name)) }
                    )
                }

                if (uiState.pairedDevices.isEmpty()) {
                    item {
                        EmptyDevicesDiscovered(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceRow(
    device: DeviceDisplayModel,
    onUnpair: () -> Unit,
    onOpenChat: () -> Unit
) {
    val semantic = MeshThemeTokens.semanticColors
    GlassPanel(modifier = Modifier.fillMaxWidth(), contentPadding = 12.dp) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = device.name,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = device.macAddress,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                MeshSignalIndicator(rssi = device.rssi)
            }
            Spacer(Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Last seen: ${device.lastSeenLabel}  •  ${device.transport}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (device.isTrusted) stringResource(R.string.contacts_trusted) else stringResource(R.string.contacts_unknown),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (device.isTrusted) semantic.connected else semantic.warning
                )
            }
            Spacer(Modifier.height(8.dp))
            // ── Action row: Chat + Unpair ──────────────────────────────────
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                MeshButton(
                    text = "Chat",
                    onClick = onOpenChat,
                    modifier = Modifier.weight(1f)
                )
                androidx.compose.material3.OutlinedButton(
                    onClick = onUnpair,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(R.string.contacts_trusted).let { "Unpair" },
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}
