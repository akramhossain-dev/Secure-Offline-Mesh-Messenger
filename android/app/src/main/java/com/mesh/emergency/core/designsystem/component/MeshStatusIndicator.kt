/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.designsystem.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mesh.emergency.core.designsystem.icon.MeshIcons
import com.mesh.emergency.core.designsystem.theme.MeshThemeTokens

/**
 * Status indicator for connection pathways (connected / disconnected / standby).
 */
@Composable
fun MeshConnectionStatus(
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    val semanticColors = MeshThemeTokens.semanticColors
    val label = if (isConnected) "Connected" else "Offline"
    val icon = if (isConnected) MeshIcons.Bluetooth else MeshIcons.BluetoothDisabled
    val color = if (isConnected) semanticColors.connected else semanticColors.offline

    MeshTag(
        text = label,
        containerColor = color.copy(alpha = 0.15f),
        contentColor = color,
        icon = icon,
        modifier = modifier
    )
}

/**
 * Battery Status tag displaying charge percent.
 */
@Composable
fun MeshBatteryStatus(
    batteryLevel: Float,
    modifier: Modifier = Modifier
) {
    val semanticColors = MeshThemeTokens.semanticColors
    val percent = (batteryLevel * 100).toInt()
    val isLow = batteryLevel < 0.2f

    val color = if (isLow) semanticColors.emergency else semanticColors.connected
    val icon = when {
        isLow -> MeshIcons.BatteryLow
        batteryLevel < 0.8f -> MeshIcons.BatteryUnknown
        else -> MeshIcons.BatteryFull
    }

    MeshTag(
        text = "$percent%",
        containerColor = color.copy(alpha = 0.15f),
        contentColor = color,
        icon = icon,
        modifier = modifier
    )
}

/**
 * Signal Strength Indicator showing active coverage tiers (Strong, Weak, Disconnected).
 */
@Composable
fun MeshSignalIndicator(
    rssi: Int,
    modifier: Modifier = Modifier
) {
    val semanticColors = MeshThemeTokens.semanticColors
    val spacing = MeshThemeTokens.spacing

    // Map typical rssi ranges
    val (icon, color, label) = when {
        rssi == 0 -> Triple(MeshIcons.SignalNone, semanticColors.offline, "No Signal")
        rssi > -70 -> Triple(MeshIcons.SignalStrong, semanticColors.connected, "Strong")
        else -> Triple(MeshIcons.SignalWeak, semanticColors.weakSignal, "Weak")
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Signal Strength",
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(spacing.xs))
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
