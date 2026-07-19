/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.mesh.emergency.core.presentation.base.PlaceholderScreen
import com.mesh.emergency.feature.chat.CommunicationScreen
import com.mesh.emergency.feature.contacts.DeviceScreen
import com.mesh.emergency.feature.dashboard.HomeScreen
import com.mesh.emergency.feature.dashboard.NetworkHealthScreen
import com.mesh.emergency.feature.dashboard.NodeVisualizationScreen
import com.mesh.emergency.feature.dashboard.SplashScreen
import com.mesh.emergency.feature.emergency.presentation.EmergencyScreen
import com.mesh.emergency.feature.map.MapScreen
import com.mesh.emergency.feature.message.presentation.ChatScreen
import com.mesh.emergency.feature.message.presentation.MessageListScreen
import com.mesh.emergency.feature.message.presentation.QrPairScreen
import com.mesh.emergency.feature.profile.ProfileScreen
import com.mesh.emergency.feature.resources.ResourceScreen
import com.mesh.emergency.feature.settings.SettingsScreen
import com.mesh.emergency.feature.settings.AboutScreen

/**
 * Main application navigation graph.
 *
 * Phase A32: MapScreen, ResourceScreen, NetworkHealthScreen, NodeVisualizationScreen wired.
 */
@Composable
fun AppNavGraph(
    navController: NavHostController,
    onOpenDrawer: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = NavigationDestination.Splash.route,
        enterTransition = NavTransitions.enterTransition,
        exitTransition = NavTransitions.exitTransition,
        popEnterTransition = NavTransitions.popEnterTransition,
        popExitTransition = NavTransitions.popExitTransition,
        modifier = modifier
    ) {
        // ── Splash ────────────────────────────────────────────────────────────
        composable(route = NavigationDestination.Splash.route) {
            SplashScreen(
                onNavigateToHome = {
                    navController.navigate(NavigationDestination.Home.route) {
                        popUpTo(NavigationDestination.Splash.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        // ── Onboarding ────────────────────────────────────────────────────────
        composable(route = NavigationDestination.Onboarding.route) {
            PlaceholderScreen(title = "Onboarding")
        }

        // ── Home Dashboard ────────────────────────────────────────────────────
        composable(route = NavigationDestination.Home.route) {
            HomeScreen(
                onOpenDrawer = onOpenDrawer,
                onNavigateToEmergency = {
                    navController.navigate(NavRoutes.EMERGENCY_DASHBOARD)
                }
            )
        }

        // ── Message List ──────────────────────────────────────────────────────
        composable(route = NavigationDestination.ChatList.route) {
            MessageListScreen(
                onOpenConversation = { id, label ->
                    navController.navigate(NavRoutes.chatScreen(id, label))
                }
            )
        }

        // ── Chat Screen ───────────────────────────────────────────────────────
        composable(
            route = NavRoutes.CHAT_SCREEN,
            arguments = listOf(
                navArgument("convId") { type = NavType.StringType },
                navArgument("label")  { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val convId = backStackEntry.arguments?.getString("convId") ?: ""
            val label  = backStackEntry.arguments?.getString("label")  ?: "Chat"
            ChatScreen(
                conversationId = convId,
                recipientLabel = label,
                onBack = { navController.popBackStack() }
            )
        }

        // ── Chat Detail (legacy deep-link) ────────────────────────────────────
        composable(
            route = NavigationDestination.ChatDetail.route,
            arguments = listOf(navArgument("contactId") { type = NavType.StringType }),
            deepLinks = listOf(navDeepLink { uriPattern = "${NavRoutes.DEEP_LINK_BASE}/chat/{contactId}" })
        ) { backStackEntry ->
            val contactId = backStackEntry.arguments?.getString("contactId") ?: "unknown"
            ChatScreen(
                conversationId = "conv-$contactId",
                recipientLabel = contactId,
                onBack = { navController.popBackStack() }
            )
        }

        // ── Global Chat ───────────────────────────────────────────────────────
        composable(route = NavigationDestination.GlobalChat.route) {
            ChatScreen(
                conversationId = "conv-broadcast",
                recipientLabel = "BROADCAST",
                onBack = { navController.popBackStack() }
            )
        }

        // ── Devices / Contacts ────────────────────────────────────────────────
        composable(route = NavigationDestination.Contacts.route) {
            DeviceScreen(
                onNavigateToQrPair = { navController.navigate(NavigationDestination.QrPair.route) }
            )
        }

        // ── QR Pair ───────────────────────────────────────────────────────────
        composable(route = NavigationDestination.QrPair.route) {
            QrPairScreen(onBack = { navController.popBackStack() })
        }

        // ── Contact Detail ────────────────────────────────────────────────────
        composable(
            route = NavigationDestination.ContactDetail.route,
            arguments = listOf(navArgument("contactId") { type = NavType.StringType })
        ) { backStackEntry ->
            val contactId = backStackEntry.arguments?.getString("contactId") ?: "unknown"
            PlaceholderScreen(title = "Contact Profile of $contactId")
        }

        // ── Emergency Dashboard ───────────────────────────────────────────────
        composable(route = NavigationDestination.Emergency.route) {
            EmergencyScreen()
        }

        // ── Emergency Dashboard (new route) ───────────────────────────────────
        composable(route = NavRoutes.EMERGENCY_DASHBOARD) {
            EmergencyScreen()
        }

        // ── SOS Active ────────────────────────────────────────────────────────
        composable(
            route = NavigationDestination.SosActive.route,
            deepLinks = listOf(navDeepLink { uriPattern = "${NavRoutes.DEEP_LINK_BASE}/sos" })
        ) {
            EmergencyScreen()
        }

        // ── Offline Map ─────────────────────────────────────────────────────────────────
        composable(route = NavigationDestination.Map.route) {
            MapScreen(onBack = { navController.popBackStack() })
        }

        // ── Network Health Dashboard (A32.4) ──────────────────────────────────────────
        composable(route = NavigationDestination.NetworkDashboard.route) {
            NetworkHealthScreen()
        }

        // ── Resource Sharing (A32.3) ────────────────────────────────────────────────
        composable(route = NavigationDestination.Resources.route) {
            ResourceScreen()
        }

        // ── Node Visualization (A32.5) ───────────────────────────────────────────────
        composable(route = NavigationDestination.Nodes.route) {
            NodeVisualizationScreen()
        }

        // ── Profile ───────────────────────────────────────────────────────────
        composable(
            route = NavigationDestination.Profile.route,
            deepLinks = listOf(navDeepLink { uriPattern = "${NavRoutes.DEEP_LINK_BASE}/profile" })
        ) {
            ProfileScreen()
        }

        // ── Settings ──────────────────────────────────────────────────────────
        composable(
            route = NavigationDestination.Settings.route,
            deepLinks = listOf(navDeepLink { uriPattern = "${NavRoutes.DEEP_LINK_BASE}/settings" })
        ) {
            SettingsScreen()
        }

        // ── About ─────────────────────────────────────────────────────────────
        composable(route = NavigationDestination.About.route) {
            AboutScreen()
        }
    }
}
