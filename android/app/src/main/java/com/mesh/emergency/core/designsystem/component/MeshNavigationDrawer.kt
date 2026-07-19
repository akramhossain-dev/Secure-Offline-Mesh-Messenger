/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mesh.emergency.R
import com.mesh.emergency.core.common.result.Result
import com.mesh.emergency.core.designsystem.icon.MeshIcons
import com.mesh.emergency.core.designsystem.theme.MeshThemeTokens
import com.mesh.emergency.core.designsystem.theme.ThemeMode
import com.mesh.emergency.feature.dashboard.DrawerViewModel

/**
 * Redesigned professional Material 3 navigation drawer sheet.
 * Incorporates:
 * - User Profile Header (nickname, ID, status)
 * - Grouped Navigation Categories (Messenger, Safety, Config)
 * - Quick Theme Toggle
 * - Quick Language Selector
 */
@Composable
fun MeshNavigationDrawerSheet(
    selectedRoute: String,
    onNavigateToRoute: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DrawerViewModel = hiltViewModel()
) {
    val currentUserResult by viewModel.currentUser.collectAsState()
    val appState by viewModel.appState.collectAsState()
    val spacing = MeshThemeTokens.spacing

    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        // ── 1. User Profile Header Card ───────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
                .padding(horizontal = spacing.xl, vertical = spacing.lg)
        ) {
            Spacer(modifier = Modifier.height(spacing.xl))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.md)
            ) {
                // Circle initials avatar
                val initials = when (val res = currentUserResult) {
                    is Result.Success -> res.data.username.take(2).uppercase()
                    else -> "ME"
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        text = initials,
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                // Profile details
                Column {
                    val name = when (val res = currentUserResult) {
                        is Result.Success -> res.data.username
                        else -> "User Profile"
                    }
                    val pubKey = when (val res = currentUserResult) {
                        is Result.Success -> "ID: " + res.data.id.take(12) + "..."
                        else -> "Initializing keys..."
                    }

                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = pubKey,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Connection Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.xs)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (appState.isOnline) MeshThemeTokens.semanticColors.connected
                                    else MeshThemeTokens.semanticColors.offline
                                )
                        )
                        Text(
                            text = if (appState.isOnline) stringResource(R.string.drawer_connected) else stringResource(R.string.drawer_offline),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(spacing.sm))

        // ── 2. Grouped Sidebar Navigation Items ─────────────────────────────
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = spacing.sm),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Categories List
            item {
                Spacer(modifier = Modifier.height(spacing.sm))
                CategoryHeader(stringResource(R.string.drawer_messenger_category))
            }
            item {
                DrawerNavigationItem(
                    label = stringResource(R.string.drawer_conversations),
                    route = "chat-list",
                    selectedRoute = selectedRoute,
                    icon = MeshIcons.Chat,
                    onClick = onNavigateToRoute
                )
            }
            item {
                DrawerNavigationItem(
                    label = stringResource(R.string.drawer_contacts_nodes),
                    route = "contacts",
                    selectedRoute = selectedRoute,
                    icon = MeshIcons.User,
                    onClick = onNavigateToRoute
                )
            }

            item {
                Spacer(modifier = Modifier.height(spacing.sm))
                CategoryHeader(stringResource(R.string.drawer_safety_category))
            }
            item {
                DrawerNavigationItem(
                    label = stringResource(R.string.drawer_emergency_sos),
                    route = "emergency",
                    selectedRoute = selectedRoute,
                    icon = MeshIcons.Emergency,
                    onClick = onNavigateToRoute
                )
            }
            item {
                DrawerNavigationItem(
                    label = stringResource(R.string.drawer_offline_map),
                    route = "map",
                    selectedRoute = selectedRoute,
                    icon = MeshIcons.Map,
                    onClick = onNavigateToRoute
                )
            }
            item {
                DrawerNavigationItem(
                    label = stringResource(R.string.drawer_resources),
                    route = "resources",
                    selectedRoute = selectedRoute,
                    icon = MeshIcons.Info,
                    onClick = onNavigateToRoute
                )
            }
            item {
                DrawerNavigationItem(
                    label = stringResource(R.string.drawer_network_status),
                    route = "network-dashboard",
                    selectedRoute = selectedRoute,
                    icon = MeshIcons.NetworkCheck,
                    onClick = onNavigateToRoute
                )
            }

            item {
                Spacer(modifier = Modifier.height(spacing.sm))
                CategoryHeader(stringResource(R.string.drawer_system_category))
            }
            item {
                DrawerNavigationItem(
                    label = stringResource(R.string.drawer_my_profile),
                    route = "profile",
                    selectedRoute = selectedRoute,
                    icon = MeshIcons.User,
                    onClick = onNavigateToRoute
                )
            }
            item {
                DrawerNavigationItem(
                    label = stringResource(R.string.drawer_app_settings),
                    route = "settings",
                    selectedRoute = selectedRoute,
                    icon = MeshIcons.Settings,
                    onClick = onNavigateToRoute
                )
            }
            item {
                DrawerNavigationItem(
                    label = stringResource(R.string.drawer_about),
                    route = "about",
                    selectedRoute = selectedRoute,
                    icon = MeshIcons.Info,
                    onClick = onNavigateToRoute
                )
            }
        }

        // ── 3. Quick Settings Action Bar (Bottom of Drawer) ───────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md)
        ) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(spacing.md))

            // Theme quick toggle row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.drawer_theme_mode),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(
                        ThemeMode.SYSTEM to "SYSTEM",
                        ThemeMode.LIGHT to "LIGHT",
                        ThemeMode.DARK to "DARK"
                    ).forEach { (mode, label) ->
                        val isSel = appState.themeMode == mode
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isSel) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                                .clickable { viewModel.setThemeMode(mode) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isSel) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(spacing.sm))

            // Language quick toggle row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.drawer_language),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(
                        "system" to "SYS",
                        "en" to "EN",
                        "bn" to "বাংলা"
                    ).forEach { (code, label) ->
                        val isSel = appState.languageCode == code
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isSel) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                                .clickable { viewModel.setLanguage(code) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isSel) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(spacing.sm))
        }
    }
}

@Composable
private fun CategoryHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        ),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

@Composable
private fun DrawerNavigationItem(
    label: String,
    route: String,
    selectedRoute: String,
    icon: ImageVector,
    onClick: (String) -> Unit
) {
    val isSelected = selectedRoute == route || 
                     (route == "chat-list" && selectedRoute.startsWith("chat"))
    
    NavigationDrawerItem(
        label = { Text(text = label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
        selected = isSelected,
        onClick = { onClick(route) },
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        shape = RoundedCornerShape(12.dp),
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
            selectedTextColor = MaterialTheme.colorScheme.primary,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}
