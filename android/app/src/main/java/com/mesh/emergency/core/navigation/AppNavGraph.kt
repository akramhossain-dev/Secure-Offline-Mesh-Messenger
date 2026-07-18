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
import com.mesh.emergency.feature.settings.SettingsScreen

/**
 * Main application navigation graph.
 *
 * Phase A26/A27: Splash, Home, Devices, Network, Communication, and Settings
 * are wired to real composable screens. Remaining destinations retain placeholder
 * screens until Phases A28–A30.
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

        // ── Onboarding (future A28) ───────────────────────────────────────────
        composable(route = NavigationDestination.Onboarding.route) {
            PlaceholderScreen(title = "Onboarding")
        }

        // ── Home Dashboard ────────────────────────────────────────────────────
        composable(route = NavigationDestination.Home.route) {
            HomeScreen(
                onNavigateToEmergency = {
                    navController.navigate(NavigationDestination.Emergency.route)
                }
            )
        }

        // ── Chat List (future A28) ────────────────────────────────────────────
        composable(route = NavigationDestination.ChatList.route) {
            CommunicationScreen()
        }

        // ── Chat Detail ───────────────────────────────────────────────────────
        composable(
            route = NavigationDestination.ChatDetail.route,
            arguments = listOf(
                navArgument("contactId") { type = NavType.StringType }
            ),
            deepLinks = listOf(
                navDeepLink { uriPattern = "${NavRoutes.DEEP_LINK_BASE}/chat/{contactId}" }
            )
        ) { backStackEntry ->
            val contactId = backStackEntry.arguments?.getString("contactId") ?: "unknown"
            PlaceholderScreen(title = "Chat with $contactId")
        }

        // ── Global Chat ───────────────────────────────────────────────────────
        composable(route = NavigationDestination.GlobalChat.route) {
            PlaceholderScreen(title = "Global Broadcast Chat")
        }

        // ── Devices / Contacts ────────────────────────────────────────────────
        composable(route = NavigationDestination.Contacts.route) {
            DeviceScreen(
                onNavigateToQrPair = {
                    navController.navigate(NavigationDestination.QrPair.route)
                }
            )
        }

        // ── QR Pair ───────────────────────────────────────────────────────────
        composable(route = NavigationDestination.QrPair.route) {
            PlaceholderScreen(title = "QR Scanner Pair")
        }

        // ── Contact Detail ────────────────────────────────────────────────────
        composable(
            route = NavigationDestination.ContactDetail.route,
            arguments = listOf(
                navArgument("contactId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val contactId = backStackEntry.arguments?.getString("contactId") ?: "unknown"
            PlaceholderScreen(title = "Contact Profile of $contactId")
        }

        // ── Emergency ─────────────────────────────────────────────────────────
        composable(route = NavigationDestination.Emergency.route) {
            PlaceholderScreen(title = "Emergency Console")
        }

        // ── SOS Active ────────────────────────────────────────────────────────
        composable(
            route = NavigationDestination.SosActive.route,
            deepLinks = listOf(
                navDeepLink { uriPattern = "${NavRoutes.DEEP_LINK_BASE}/sos" }
            )
        ) {
            PlaceholderScreen(title = "SOS Emergency Active")
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
            deepLinks = listOf(
                navDeepLink { uriPattern = "${NavRoutes.DEEP_LINK_BASE}/profile" }
            )
        ) {
            PlaceholderScreen(title = "My Profile")
        }

        // ── Settings ──────────────────────────────────────────────────────────
        composable(
            route = NavigationDestination.Settings.route,
            deepLinks = listOf(
                navDeepLink { uriPattern = "${NavRoutes.DEEP_LINK_BASE}/settings" }
            )
        ) {
            SettingsScreen()
        }

        // ── About ─────────────────────────────────────────────────────────────
        composable(route = NavigationDestination.About.route) {
            PlaceholderScreen(title = "About App")
        }
    }
}
