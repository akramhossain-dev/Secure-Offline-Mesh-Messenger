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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mesh.emergency.core.designsystem.icon.MeshIcons
import com.mesh.emergency.core.designsystem.theme.MeshThemeTokens

/**
 * Centered error layout screen containing details and action callback hooks.
 */
@Composable
fun MeshErrorState(
    title: String,
    description: String,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
    actionLabel: String = "Retry"
) {
    val spacing = MeshThemeTokens.spacing
    val semanticColors = MeshThemeTokens.semanticColors

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxSize()
            .padding(spacing.screenHorizontal)
    ) {
        Icon(
            imageVector = MeshIcons.Error,
            contentDescription = null,
            tint = semanticColors.emergency,
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
        Spacer(modifier = Modifier.height(spacing.xxl))
        MeshButton(
            text = actionLabel,
            onClick = onActionClick,
            icon = MeshIcons.Refresh
        )
    }
}

/** Pre-configured ErrorState displayed when local system permissions have been denied. */
@Composable
fun PermissionRequiredState(
    permissionName: String,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    MeshErrorState(
        title = "Permission Required",
        description = "The app requires $permissionName permission to operate in offline environments.",
        actionLabel = "Grant Permission",
        onActionClick = onRequestPermission,
        modifier = modifier
    )
}

/** Pre-configured ErrorState displayed when Bluetooth transceiver is powered down. */
@Composable
fun BluetoothDisabledState(
    onEnableBluetooth: () -> Unit,
    modifier: Modifier = Modifier
) {
    MeshErrorState(
        title = "Bluetooth is Powered Down",
        description = "Turn on the device's Bluetooth transceiver to bridge with local mesh node hardware.",
        actionLabel = "Enable Bluetooth",
        onActionClick = onEnableBluetooth,
        modifier = modifier
    )
}
