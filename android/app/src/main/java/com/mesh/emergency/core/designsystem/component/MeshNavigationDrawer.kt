/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.designsystem.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.mesh.emergency.core.designsystem.icon.MeshIcons
import com.mesh.emergency.core.designsystem.theme.MeshThemeTokens

/**
 * modal navigation drawer sheet template.
 */
@Composable
fun MeshNavigationDrawerSheet(
    selectedRoute: String,
    onNavigateToRoute: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = MeshThemeTokens.spacing
    val items = listOf(
        DrawerItem.Profile,
        DrawerItem.Emergency,
        DrawerItem.Resources,
        DrawerItem.Settings,
        DrawerItem.About
    )

    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Spacer(modifier = Modifier.height(spacing.xxl))
        Text(
            text = "Emergency Mesh",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = spacing.xxl, vertical = spacing.sm)
        )
        Spacer(modifier = Modifier.height(spacing.md))

        items.forEach { item ->
            val isSelected = selectedRoute == item.route
            NavigationDrawerItem(
                label = { Text(text = item.label) },
                selected = isSelected,
                onClick = { onNavigateToRoute(item.route) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label
                    )
                },
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }
    }
}

private sealed class DrawerItem(
    val route: String,
    val icon: ImageVector,
    val label: String
) {
    data object Profile : DrawerItem("profile", MeshIcons.User, "Profile")
    data object Emergency : DrawerItem("emergency", MeshIcons.Emergency, "Emergency SOS")
    data object Resources : DrawerItem("resources", MeshIcons.Info, "Resources Board")
    data object Settings : DrawerItem("settings", MeshIcons.Settings, "Settings")
    data object About : DrawerItem("about", MeshIcons.Info, "About Application")
}
