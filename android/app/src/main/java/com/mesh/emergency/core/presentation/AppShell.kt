/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.presentation

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mesh.emergency.core.designsystem.component.MeshNavigationBar
import com.mesh.emergency.core.designsystem.component.MeshNavigationDrawerSheet
import com.mesh.emergency.core.designsystem.icon.MeshIcons
import com.mesh.emergency.core.navigation.AppNavGraph
import com.mesh.emergency.core.navigation.AppNavigator
import com.mesh.emergency.core.navigation.NavigationAction
import com.mesh.emergency.core.presentation.adaptive.WindowSize
import com.mesh.emergency.core.presentation.adaptive.rememberWindowSize
import kotlinx.coroutines.launch

/**
 * Root Application Shell enclosing the global layout scaffolding.
 * Listens to [AppNavigator] events, maps active bottom bars,
 * and updates layouts adaptively for tablets/phones.
 */
@Composable
fun AppShell(
    appNavigator: AppNavigator,
    messageNotifier: com.mesh.emergency.core.notification.MessageNotifier,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val windowSize = rememberWindowSize()
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    val localScope = rememberCoroutineScope()

    // ── Collect message notifications when app is open ───────────────────────
    LaunchedEffect(messageNotifier, navController) {
        messageNotifier.events.collect { event ->
            val messageText: String
            val convId: String
            val label: String
            val isGlobal: Boolean
            when (event) {
                is com.mesh.emergency.core.notification.MessageNotifier.Event.PrivateMessage -> {
                    messageText = "New message from ${event.senderName}: ${event.text}"
                    convId = event.senderId
                    label = event.senderName
                    isGlobal = false
                }
                is com.mesh.emergency.core.notification.MessageNotifier.Event.GlobalMessage -> {
                    messageText = "[Global] ${event.senderName}: ${event.text}"
                    convId = "global"
                    label = "Global Chat"
                    isGlobal = true
                }
            }

            localScope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = messageText,
                    actionLabel = "Reply",
                    duration = androidx.compose.material3.SnackbarDuration.Short
                )
                if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                    if (isGlobal) {
                        navController.navigate(com.mesh.emergency.core.navigation.NavRoutes.GLOBAL_CHAT)
                    } else {
                        navController.navigate(com.mesh.emergency.core.navigation.NavRoutes.chatScreen(convId, label))
                    }
                }
            }
        }
    }

    // ── Bind AppNavigator actions to the NavHost ───────────────────────────
    LaunchedEffect(navController, appNavigator) {
        appNavigator.navigationActions.collect { action ->
            when (action) {
                is NavigationAction.NavigateTo -> {
                    navController.navigate(action.destination) {
                        action.popUpToRoute?.let { popUpTo(it) { inclusive = action.inclusive } }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
                is NavigationAction.NavigateBack -> {
                    navController.popBackStack()
                }
            }
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: "splash"

    // Top-level destinations mapping for bottom bar highlight checks
    val isTopLevelRoute = currentRoute in listOf(
        "home",
        "chat-list",
        "global-chat",
        "map",
        "network-dashboard",
        "settings"
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = isTopLevelRoute,
        drawerContent = {
            MeshNavigationDrawerSheet(
                selectedRoute = currentRoute,
                onNavigateToRoute = { route ->
                    coroutineScope.launch { drawerState.close() }
                    navController.navigate(route) {
                        popUpTo("home") { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) {
        Row(modifier = modifier.fillMaxSize()) {
            // Adaptive Rails navigation layout for Tablet/Landscape
            if (windowSize != WindowSize.COMPACT && isTopLevelRoute) {
                NavigationRail(
                    modifier = Modifier.fillMaxHeight()
                ) {
                    val railItems = listOf(
                        RailItem.Home,
                        RailItem.Chats,
                        RailItem.Map,
                        RailItem.Network,
                        RailItem.Settings
                    )
                    railItems.forEach { item ->
                        val isSelected = currentRoute == item.route
                        NavigationRailItem(
                            selected = isSelected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo("home") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(text = item.label) }
                        )
                    }
                }
            }

            @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
            val isKeyboardVisible = WindowInsets.isImeVisible

            Scaffold(
                contentWindowInsets = if (isKeyboardVisible) {
                    WindowInsets.statusBars
                } else {
                    WindowInsets.systemBars
                },
                snackbarHost = { androidx.compose.material3.SnackbarHost(hostState = snackbarHostState) },
                bottomBar = {
                    // Mobile navigation layout
                    if (windowSize == WindowSize.COMPACT && isTopLevelRoute && !isKeyboardVisible) {
                        MeshNavigationBar(
                            selectedRoute = currentRoute,
                            onNavigateToRoute = { route ->
                                navController.navigate(route) {
                                    popUpTo("home") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                },
                modifier = Modifier.weight(1f)
            ) { paddingValues ->
                AppNavGraph(
                    navController = navController,
                    onOpenDrawer = {
                        coroutineScope.launch { drawerState.open() }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = paddingValues.calculateBottomPadding())
                )
            }
        }
    }
}

private sealed class RailItem(
    val route: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String
) {
    data object Home : RailItem("home", MeshIcons.Home, "Home")
    data object Chats : RailItem("chat-list", MeshIcons.Chat, "Chats")
    data object Map : RailItem("map", MeshIcons.Map, "Map")
    data object Network : RailItem("network-dashboard", MeshIcons.NetworkCheck, "Network")
    data object Settings : RailItem("settings", MeshIcons.Settings, "Settings")
}
