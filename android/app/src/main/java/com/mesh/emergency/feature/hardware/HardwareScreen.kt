/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.feature.hardware

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mesh.emergency.core.designsystem.component.AuroraBackdrop
import com.mesh.emergency.core.designsystem.component.GlassPanel
import com.mesh.emergency.core.designsystem.component.GlassPanelVariant
import com.mesh.emergency.core.designsystem.component.MeshButton
import com.mesh.emergency.core.designsystem.component.MeshOutlinedButton
import com.mesh.emergency.core.designsystem.component.SignalStrengthBars
import com.mesh.emergency.core.designsystem.theme.MeshThemeTokens
import com.mesh.emergency.core.hardware.BleConnectionState
import com.mesh.emergency.core.hardware.BleDevice
import com.mesh.emergency.core.hardware.BleDiscoveryState
import com.mesh.emergency.core.hardware.HardwareCapability
import com.mesh.emergency.core.hardware.HardwareDeviceProfile
import com.mesh.emergency.core.hardware.HardwareDeviceType

/**
 * Hardware / BLE Pairing screen — A30 discovery and connection UI.
 *
 * Shows:
 * - Scanning animation + status
 * - List of discovered compatible BLE nodes with RSSI signal bars
 * - Connected hardware profile (battery, signal, firmware, capabilities)
 * - Start/Stop scan controls
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HardwareScreen(
    viewModel: HardwareViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val spacing = MeshThemeTokens.spacing
    val semanticColors = MeshThemeTokens.semanticColors

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is HardwareUiEffect.ShowMessage       -> snackbarHostState.showSnackbar(effect.text)
                is HardwareUiEffect.ConnectionSuccess -> snackbarHostState.showSnackbar("✓ Connected to ${effect.name}")
                is HardwareUiEffect.ConnectionFailed  -> snackbarHostState.showSnackbar("✗ Connection failed: ${effect.reason}")
                HardwareUiEffect.Disconnected         -> snackbarHostState.showSnackbar("Disconnected from hardware node")
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
                                text = "Hardware Bridge",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "BLE / ESP32 Pairing",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.onEvent(HardwareUiEvent.RefreshHardwareStatus) }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
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

                // ── Discovery Controls ────────────────────────────────────────
                item {
                    DiscoveryControlPanel(
                        discoveryState = uiState.discoveryState,
                        onStartScan = { viewModel.onEvent(HardwareUiEvent.StartScan) },
                        onStopScan  = { viewModel.onEvent(HardwareUiEvent.StopScan) }
                    )
                }

                // ── Scan progress indicator ───────────────────────────────────
                item {
                    AnimatedVisibility(
                        visible = uiState.discoveryState == BleDiscoveryState.SCANNING,
                        enter = fadeIn(), exit = fadeOut()
                    ) {
                        Column {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Scanning for Mesh nodes (${uiState.discoveredDevices.size} found)…",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // ── Connected Hardware Profile ────────────────────────────────
                uiState.connectedProfile?.let { profile ->
                    item {
                        ConnectedProfileCard(
                            profile = profile,
                            onDisconnect = { viewModel.onEvent(HardwareUiEvent.DisconnectDevice) },
                            onSendStatus = { viewModel.onEvent(HardwareUiEvent.SendGetStatusCommand) }
                        )
                    }
                }

                // ── Discovered Devices ────────────────────────────────────────
                if (uiState.discoveredDevices.isNotEmpty()) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.BluetoothSearching, null, tint = semanticColors.info, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Nearby Mesh Nodes (${uiState.discoveredDevices.size})",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }

                    items(uiState.discoveredDevices, key = { it.id }) { device ->
                        BleDeviceCard(
                            device = device,
                            isConnecting = uiState.isConnecting && uiState.selectedDevice?.id == device.id,
                            onConnect = {
                                viewModel.onEvent(HardwareUiEvent.SelectDevice(device))
                                viewModel.onEvent(HardwareUiEvent.ConnectDevice(device.macAddress))
                            }
                        )
                    }
                } else if (uiState.discoveryState == BleDiscoveryState.STOPPED ||
                           uiState.discoveryState == BleDiscoveryState.IDLE) {
                    item {
                        GlassPanel(modifier = Modifier.fillMaxWidth()) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.Bluetooth, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(36.dp))
                                Spacer(Modifier.height(spacing.sm))
                                Text(
                                    text = "No mesh nodes found",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Ensure ESP32 node is powered on and in range",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // ── Protocol Info card ────────────────────────────────────────
                item {
                    GlassPanel(modifier = Modifier.fillMaxWidth(), variant = GlassPanelVariant.WARNING) {
                        Text(
                            text = "BLE bridge targets ESP32 + SX1278 (Service UUID: FF10). " +
                                "Firmware must support Mesh GATT profile. " +
                                "Real LoRa transmission is handled by Phase A32.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Discovery Control Panel
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DiscoveryControlPanel(
    discoveryState: BleDiscoveryState,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit
) {
    val semanticColors = MeshThemeTokens.semanticColors
    val spacing = MeshThemeTokens.spacing
    val isScanning = discoveryState == BleDiscoveryState.SCANNING

    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Animated scanner icon
            val infiniteTransition = rememberInfiniteTransition(label = "scan_rotate")
            val rotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue  = if (isScanning) 360f else 0f,
                animationSpec = infiniteRepeatable(
                    tween(2000, easing = LinearEasing),
                    RepeatMode.Restart
                ),
                label = "rotation"
            )

            Icon(
                imageVector = when (discoveryState) {
                    BleDiscoveryState.SCANNING          -> Icons.Default.BluetoothSearching
                    BleDiscoveryState.BLE_UNAVAILABLE   -> Icons.Default.BluetoothDisabled
                    else                                -> Icons.Default.Bluetooth
                },
                contentDescription = "BLE Status",
                tint = when (discoveryState) {
                    BleDiscoveryState.SCANNING        -> semanticColors.info
                    BleDiscoveryState.BLE_UNAVAILABLE -> semanticColors.offline
                    else                              -> MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier
                    .size(28.dp)
                    .rotate(if (isScanning) rotation else 0f)
            )

            Spacer(Modifier.width(spacing.md))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (discoveryState) {
                        BleDiscoveryState.SCANNING          -> "Scanning…"
                        BleDiscoveryState.STOPPED           -> "Scan stopped"
                        BleDiscoveryState.PERMISSION_DENIED -> "Permission denied"
                        BleDiscoveryState.BLE_UNAVAILABLE   -> "Bluetooth unavailable"
                        BleDiscoveryState.IDLE              -> "Ready to scan"
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = "Mesh Service UUID: 0000FF10",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isScanning) {
                MeshOutlinedButton(text = "Stop", onClick = onStopScan)
            } else {
                MeshButton(text = "Scan", onClick = onStartScan)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BLE Device Card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BleDeviceCard(
    device: BleDevice,
    isConnecting: Boolean,
    onConnect: () -> Unit
) {
    val semanticColors = MeshThemeTokens.semanticColors
    val spacing = MeshThemeTokens.spacing

    val connectionColor = when (device.connectionState) {
        BleConnectionState.CONNECTED     -> semanticColors.connected
        BleConnectionState.CONNECTING    -> semanticColors.warning
        BleConnectionState.FAILED        -> semanticColors.emergency
        else                             -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = 12.dp,
        variant = if (device.connectionState == BleConnectionState.CONNECTED) GlassPanelVariant.SUCCESS else GlassPanelVariant.DEFAULT
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = if (device.connectionState == BleConnectionState.CONNECTED)
                    Icons.Default.BluetoothConnected else Icons.Default.Bluetooth,
                contentDescription = null,
                tint = connectionColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = device.macAddress,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = device.connectionState.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = connectionColor
                )
            }
            Spacer(Modifier.width(spacing.sm))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                SignalStrengthBars(rssi = device.rssi)
                Text(
                    text = "${device.rssi} dBm",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(spacing.sm))
            if (device.connectionState != BleConnectionState.CONNECTED) {
                MeshButton(
                    text = if (isConnecting) "…" else "Connect",
                    onClick = onConnect,
                    enabled = !isConnecting
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Connected Hardware Profile Card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ConnectedProfileCard(
    profile: HardwareDeviceProfile,
    onDisconnect: () -> Unit,
    onSendStatus: () -> Unit
) {
    val semanticColors = MeshThemeTokens.semanticColors
    val spacing = MeshThemeTokens.spacing

    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        variant = GlassPanelVariant.SUCCESS,
        contentPadding = 14.dp
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Memory,
                    contentDescription = "Hardware",
                    tint = semanticColors.success,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = when (profile.deviceType) {
                        HardwareDeviceType.ESP32_LORA  -> "ESP32 + LoRa Node"
                        HardwareDeviceType.ESP32_ONLY  -> "ESP32 Node"
                        HardwareDeviceType.GENERIC_BLE -> "Generic BLE Device"
                        HardwareDeviceType.UNKNOWN     -> "Unknown Device"
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = semanticColors.success
                )
            }

            Spacer(Modifier.height(spacing.sm))

            // Hardware stats grid
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.md),
                modifier = Modifier.fillMaxWidth()
            ) {
                HardwareStatItem(label = "Battery", value = "${profile.batteryPercent}%", modifier = Modifier.weight(1f))
                HardwareStatItem(label = "RSSI", value = "${profile.signalRssi} dBm", modifier = Modifier.weight(1f))
                HardwareStatItem(label = "FW", value = profile.firmwareVersion, modifier = Modifier.weight(1f))
            }

            Spacer(Modifier.height(spacing.sm))

            // Capabilities
            val capabilityLabels = profile.capabilities.map { cap ->
                when (cap) {
                    HardwareCapability.LORA_TX       -> "LoRa TX"
                    HardwareCapability.LORA_RX       -> "LoRa RX"
                    HardwareCapability.GPS           -> "GPS"
                    HardwareCapability.BATTERY_MONITOR -> "Batt. Monitor"
                    HardwareCapability.MESH_RELAY    -> "Mesh Relay"
                }
            }
            if (capabilityLabels.isNotEmpty()) {
                Text(
                    text = "Capabilities: ${capabilityLabels.joinToString(" · ")}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(spacing.sm))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                MeshOutlinedButton(
                    text = "GET_STATUS",
                    onClick = onSendStatus,
                    modifier = Modifier.weight(1f)
                )
                MeshOutlinedButton(
                    text = "Disconnect",
                    onClick = onDisconnect,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HardwareStatItem(label: String, value: String, modifier: Modifier = Modifier) {
    GlassPanel(modifier = modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
