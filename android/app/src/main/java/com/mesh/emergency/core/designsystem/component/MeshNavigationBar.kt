/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.designsystem.component

import androidx.annotation.StringRes
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.mesh.emergency.R
import com.mesh.emergency.core.designsystem.icon.MeshIcons

/**
 * Bottom navigation bar enclosing destination items.
 */
@Composable
fun MeshNavigationBar(
    selectedRoute: String,
    onNavigateToRoute: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        NavigationItem.Home,
        NavigationItem.Chats,
        NavigationItem.GlobalChat,
        NavigationItem.Network,
        NavigationItem.Settings
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        items.forEach { item ->
            val isSelected = selectedRoute == item.route
            val label = stringResource(item.labelResId)
            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigateToRoute(item.route) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = label
                    )
                },
                label = {
                    Text(text = label, style = MaterialTheme.typography.labelSmall)
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

private sealed class NavigationItem(
    val route: String,
    val icon: ImageVector,
    @StringRes val labelResId: Int
) {
    data object Home       : NavigationItem("home",              MeshIcons.Home,         R.string.nav_home_label)
    data object Chats      : NavigationItem("chat-list",         MeshIcons.Chat,         R.string.nav_chats_label)
    data object GlobalChat : NavigationItem("global-chat",       MeshIcons.Global,       R.string.nav_global_label)
    data object Network    : NavigationItem("network-dashboard", MeshIcons.NetworkCheck, R.string.nav_network_label)
    data object Settings   : NavigationItem("settings",          MeshIcons.Settings,     R.string.nav_settings_label)
}
