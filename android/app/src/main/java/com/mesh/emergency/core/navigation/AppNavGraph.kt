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

/**
 * Main application navigation graph mapping routes and deep-links to placeholder screens.
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
            PlaceholderScreen(title = "Splash")
        }

        // ── Onboarding ────────────────────────────────────────────────────────
        composable(route = NavigationDestination.Onboarding.route) {
            PlaceholderScreen(title = "Onboarding")
        }

        // ── Home ──────────────────────────────────────────────────────────────
        composable(route = NavigationDestination.Home.route) {
            PlaceholderScreen(title = "Home")
        }

        // ── Chat List ─────────────────────────────────────────────────────────
        composable(route = NavigationDestination.ChatList.route) {
            PlaceholderScreen(title = "Chats")
        }

        // ── Chat Detail (with parameter and deep link support) ────────────────
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
            PlaceholderScreen(title = "Chat Session with $contactId")
        }

        // ── Global Chat ───────────────────────────────────────────────────────
        composable(route = NavigationDestination.GlobalChat.route) {
            PlaceholderScreen(title = "Global Broadcast Chat")
        }

        // ── Contacts ──────────────────────────────────────────────────────────
        composable(route = NavigationDestination.Contacts.route) {
            PlaceholderScreen(title = "Contacts")
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

        // ── SOS Active (with deep link support) ──────────────────────────────
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
            PlaceholderScreen(title = "Network Diagnostics")
        }

        // ── Profile (with deep link support) ──────────────────────────────────
        composable(
            route = NavigationDestination.Profile.route,
            deepLinks = listOf(
                navDeepLink { uriPattern = "${NavRoutes.DEEP_LINK_BASE}/profile" }
            )
        ) {
            PlaceholderScreen(title = "My Profile")
        }

        // ── Settings (with deep link support) ─────────────────────────────────
        composable(
            route = NavigationDestination.Settings.route,
            deepLinks = listOf(
                navDeepLink { uriPattern = "${NavRoutes.DEEP_LINK_BASE}/settings" }
            )
        ) {
            PlaceholderScreen(title = "Settings")
        }

        // ── About ─────────────────────────────────────────────────────────────
        composable(route = NavigationDestination.About.route) {
            PlaceholderScreen(title = "About App")
        }
    }
}
