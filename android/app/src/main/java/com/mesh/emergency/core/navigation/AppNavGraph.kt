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
import com.mesh.emergency.feature.dashboard.NetworkScreen
import com.mesh.emergency.feature.dashboard.SplashScreen
import com.mesh.emergency.feature.emergency.presentation.EmergencyScreen
import com.mesh.emergency.feature.message.presentation.ChatScreen
import com.mesh.emergency.feature.message.presentation.MessageListScreen
import com.mesh.emergency.feature.settings.SettingsScreen

/**
 * Main application navigation graph.
 *
 * Phase A28/A29: Emergency, MessageList, and Chat screens are wired.
 * Remaining destinations retain placeholder screens until Phases A30+.
 */
@Composable
fun AppNavGraph(
    navController: NavHostController,
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
            PlaceholderScreen(title = "QR Scanner Pair")
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

        // ── Offline Map ───────────────────────────────────────────────────────
        composable(route = NavigationDestination.Map.route) {
            PlaceholderScreen(title = "Offline Maps")
        }

        // ── Network Dashboard ─────────────────────────────────────────────────
        composable(route = NavigationDestination.NetworkDashboard.route) {
            NetworkScreen()
        }

        // ── Profile ───────────────────────────────────────────────────────────
        composable(
            route = NavigationDestination.Profile.route,
            deepLinks = listOf(navDeepLink { uriPattern = "${NavRoutes.DEEP_LINK_BASE}/profile" })
        ) {
            PlaceholderScreen(title = "My Profile")
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
            PlaceholderScreen(title = "About App")
        }
    }
}
