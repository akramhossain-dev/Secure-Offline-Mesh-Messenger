/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency

import com.mesh.emergency.core.navigation.NavRoutes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests validating navigation route definitions and deep link patterns.
 */
class NavigationTest {

    @Test
    fun navRoutes_splash_routeIsCorrect() {
        assertEquals("splash", NavRoutes.SPLASH)
    }

    @Test
    fun navRoutes_home_routeIsCorrect() {
        assertEquals("home", NavRoutes.HOME)
    }

    @Test
    fun navRoutes_settings_routeIsCorrect() {
        assertEquals("settings", NavRoutes.SETTINGS)
    }

    @Test
    fun navRoutes_networkDashboard_routeIsCorrect() {
        assertEquals("network-dashboard", NavRoutes.NETWORK_DASHBOARD)
    }

    @Test
    fun navRoutes_deepLinkBase_isCorrectScheme() {
        assertTrue(NavRoutes.DEEP_LINK_BASE.startsWith("mesh://"))
    }

    @Test
    fun navRoutes_chatDetail_builderReplacesContactId() {
        val route = NavRoutes.chatDetail("user-abc-123")
        assertEquals("chat/user-abc-123", route)
    }

    @Test
    fun navRoutes_contactDetail_builderReplacesContactId() {
        val route = NavRoutes.contactDetail("node-xyz")
        assertEquals("contacts/node-xyz", route)
    }

    @Test
    fun navRoutes_allTopLevelRoutes_areUnique() {
        val routes = listOf(
            NavRoutes.SPLASH,
            NavRoutes.HOME,
            NavRoutes.CHAT_LIST,
            NavRoutes.CONTACTS,
            NavRoutes.MAP,
            NavRoutes.NETWORK_DASHBOARD,
            NavRoutes.SETTINGS
        )
        assertEquals(routes.size, routes.toSet().size)
    }

    @Test
    fun navRoutes_deepLinks_followMeshScheme() {
        val deepLink = "${NavRoutes.DEEP_LINK_BASE}/sos"
        assertTrue(deepLink.startsWith("mesh://emergency"))
    }
}
