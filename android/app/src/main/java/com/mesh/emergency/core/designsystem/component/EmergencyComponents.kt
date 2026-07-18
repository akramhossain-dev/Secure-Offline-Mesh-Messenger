/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mesh.emergency.core.designsystem.icon.MeshIcons
import com.mesh.emergency.core.designsystem.theme.MeshThemeTokens

// ─────────────────────────────────────────────────────────────────────────────
// Emergency Component Library — A25.7
//
// These components are tailored for emergency communication contexts:
//   - High contrast, clear hierarchy
//   - Immediate visual recognition
//   - No decorative animations that compete with status information
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Network status card showing transport type, connection state, and node info.
 *
 * @param transportLabel  Label of the active transport (e.g. "Bluetooth", "LoRa")
 * @param isConnected     Whether a node is currently reachable.
 * @param nodeCount       Number of visible nodes (0 = no mesh).
 */
@Composable
fun NetworkStatusCard(
    transportLabel: String,
    isConnected: Boolean,
    nodeCount: Int,
    modifier: Modifier = Modifier
) {
    val semanticColors = MeshThemeTokens.semanticColors
    val activeColor = if (isConnected) semanticColors.connected else semanticColors.offline

    GlassPanel(
        modifier = modifier.fillMaxWidth(),
        variant = GlassPanelVariant.DEFAULT,
        contentPadding = 12.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = if (isConnected) MeshIcons.Bluetooth else MeshIcons.BluetoothDisabled,
                contentDescription = "Transport",
                tint = activeColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transportLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isConnected) "$nodeCount node(s) visible" else "No nodes found",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            MeshConnectionStatus(isConnected = isConnected)
        }
    }
}

/**
 * Battery status card showing charge level with visual alert when low.
 *
 * @param batteryLevel  Float 0.0–1.0
 * @param isCharging    Whether an external charger is connected.
 */
@Composable
fun BatteryStatusCard(
    batteryLevel: Float,
    isCharging: Boolean,
    modifier: Modifier = Modifier
) {
    val isLow = batteryLevel < 0.2f
    val variant = if (isLow) GlassPanelVariant.WARNING else GlassPanelVariant.DEFAULT

    GlassPanel(
        modifier = modifier.fillMaxWidth(),
        variant = variant,
        contentPadding = 12.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(
                    text = "Battery",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (isCharging) {
                    Text(
                        text = "Charging",
                        style = MaterialTheme.typography.labelSmall,
                        color = MeshThemeTokens.semanticColors.success
                    )
                }
            }
            MeshBatteryStatus(batteryLevel = batteryLevel)
        }
    }
}

/**
 * SOS indicator banner shown when an active emergency event is in progress.
 * Displays with an emergency glass tint and pulsing ring.
 *
 * @param isActive      Whether an SOS is currently triggered.
 * @param messageText   Short description text for the active emergency.
 */
@Composable
fun SosIndicatorBanner(
    isActive: Boolean,
    messageText: String = "SOS Active",
    modifier: Modifier = Modifier
) {
    if (!isActive) return

    val semanticColors = MeshThemeTokens.semanticColors

    GlassPanel(
        modifier = modifier.fillMaxWidth(),
        variant = GlassPanelVariant.EMERGENCY,
        contentPadding = 16.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            PulsingRing(color = semanticColors.emergency, size = 20.dp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = "EMERGENCY",
                    style = MaterialTheme.typography.labelLarge,
                    color = semanticColors.emergency
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = messageText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.weight(1f))
            PriorityBadge(level = AlertPriorityLevel.CRITICAL)
        }
    }
}
