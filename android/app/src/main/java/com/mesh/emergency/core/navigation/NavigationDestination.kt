/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.navigation

/**
 * Encapsulates strongly-typed navigation destinations, mapping logical routes
 * to URL parameters.
 */
sealed class NavigationDestination(val route: String) {

    /** Splash screen. */
    data object Splash : NavigationDestination(NavRoutes.SPLASH)

    /** App first-launch configuration flow. */
    data object Onboarding : NavigationDestination(NavRoutes.ONBOARDING)

    /** Container home route dashboard. */
    data object Home : NavigationDestination(NavRoutes.HOME)

    /** Message conversation history list view. */
    data object ChatList : NavigationDestination(NavRoutes.CHAT_LIST)

    /** Active peer chat conversation screen. */
    data object ChatDetail : NavigationDestination(NavRoutes.CHAT_DETAIL) {
        /** Create a parameterized chat detail navigation path. */
        fun createRoute(contactId: String): String = NavRoutes.chatDetail(contactId)
    }

    /** Broadcast chat conversation screen. */
    data object GlobalChat : NavigationDestination(NavRoutes.GLOBAL_CHAT)

    /** Discovered and paired peers contact roster list. */
    data object Contacts : NavigationDestination(NavRoutes.CONTACTS)

    /** QR scanner interface for pair handshakes. */
    data object QrPair : NavigationDestination(NavRoutes.QR_PAIR)

    /** Details page for a specific contact. */
    data object ContactDetail : NavigationDestination(NavRoutes.CONTACT_DETAIL) {
        /** Create a parameterized contact detail navigation path. */
        fun createRoute(contactId: String): String = NavRoutes.contactDetail(contactId)
    }

    /** Configuration view to broadcast emergency distress beacons. */
    data object Emergency : NavigationDestination(NavRoutes.EMERGENCY)

    /** Active SOS overlay. */
    data object SosActive : NavigationDestination(NavRoutes.SOS_ACTIVE)

    /** Offline geographic map screen overlaying nearby contacts. */
    data object Map : NavigationDestination(NavRoutes.MAP)

    /** Node network visualizer displaying route hops. */
    data object NetworkDashboard : NavigationDestination(NavRoutes.NETWORK_DASHBOARD)

    /** Config details showing profile nickname and QR code cards. */
    data object Profile : NavigationDestination(NavRoutes.PROFILE)

    /** Language configurations, theme customization settings. */
    data object Settings : NavigationDestination(NavRoutes.SETTINGS)

    /** Legal notices, open source licenses, build metadata. */
    data object About : NavigationDestination(NavRoutes.ABOUT)
}
