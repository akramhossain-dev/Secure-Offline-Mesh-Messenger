/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mesh.emergency.core.designsystem.icon.MeshIcons
import com.mesh.emergency.core.designsystem.theme.MeshThemeTokens

/**
 * Centered layout placeholder displayed when a screen has empty lists (e.g. no messages or nodes).
 */
@Composable
fun MeshEmptyState(
    title: String,
    description: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    val spacing = MeshThemeTokens.spacing

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxSize()
            .padding(spacing.screenHorizontal)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(spacing.md))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(spacing.xs))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/** Pre-configured EmptyState shown when a chat history has no messages. */
@Composable
fun EmptyChatHistory(modifier: Modifier = Modifier) {
    MeshEmptyState(
        title = "No Messages",
        description = "Send a text or hold the voice note action to start a conversation.",
        icon = MeshIcons.Chat,
        modifier = modifier
    )
}

/** Pre-configured EmptyState shown when the BLE scanner returns no nearby items. */
@Composable
fun EmptyDevicesDiscovered(modifier: Modifier = Modifier) {
    MeshEmptyState(
        title = "No Mesh Nodes Found",
        description = "Verify that the local ESP32 hardware node is powered on and within Bluetooth BLE range.",
        icon = MeshIcons.BluetoothDisabled,
        modifier = modifier
    )
}

/** Pre-configured EmptyState shown when the SOS history tab is clean. */
@Composable
fun EmptyEmergencyAlerts(modifier: Modifier = Modifier) {
    MeshEmptyState(
        title = "No Active Emergencies",
        description = "No SOS distress beacons detected on the LoRa mesh network.",
        icon = MeshIcons.Emergency,
        modifier = modifier
    )
}
