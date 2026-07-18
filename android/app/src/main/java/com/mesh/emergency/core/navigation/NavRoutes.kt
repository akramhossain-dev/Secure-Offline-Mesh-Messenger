/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.navigation

/**
 * Navigation route definitions for the Offline Emergency Mesh Communication System.
 *
 * Phase A1: Route string constants only — no NavHost or NavController.
 * Phase A2: NavHost with full navigation graph will be implemented
 *           in MainActivity referencing these constants.
 *
 * Route design principles:
 * - All routes are lowercase with hyphens (URL-style)
 * - Argument routes use {paramName} placeholders
 * - Deep link URIs follow: mesh://emergency/<route>
 */
object NavRoutes {

    /** Base deep link URI scheme for the app. */
    const val DEEP_LINK_BASE = "mesh://emergency"

    // ─────────────────────────────────────────────────────────────────────────
    // Top-level routes
    // ─────────────────────────────────────────────────────────────────────────

    const val SPLASH        = "splash"
    const val ONBOARDING    = "onboarding"
    const val HOME          = "home"

    // ─────────────────────────────────────────────────────────────────────────
    // Chat feature routes
    // ─────────────────────────────────────────────────────────────────────────

    const val CHAT_LIST     = "chat-list"
    const val CHAT_DETAIL   = "chat/{contactId}"
    const val GLOBAL_CHAT   = "global-chat"

    /** Builds the [CHAT_DETAIL] route for a specific [contactId]. */
    fun chatDetail(contactId: String) = "chat/$contactId"

    // ─────────────────────────────────────────────────────────────────────────
    // Contacts feature routes
    // ─────────────────────────────────────────────────────────────────────────

    const val CONTACTS      = "contacts"
    const val QR_PAIR       = "qr-pair"
    const val CONTACT_DETAIL = "contacts/{contactId}"

    fun contactDetail(contactId: String) = "contacts/$contactId"

    // ─────────────────────────────────────────────────────────────────────────
    // Emergency feature routes
    // ─────────────────────────────────────────────────────────────────────────

    const val EMERGENCY     = "emergency"
    const val SOS_ACTIVE    = "sos-active"

    // ─────────────────────────────────────────────────────────────────────────
    // Map feature routes
    // ─────────────────────────────────────────────────────────────────────────

    const val MAP           = "map"

    // ─────────────────────────────────────────────────────────────────────────
    // Resource feature routes (A32.3)
    // ─────────────────────────────────────────────────────────────────────────

    const val RESOURCES     = "resources"

    // ─────────────────────────────────────────────────────────────────────────
    // Node visualization routes (A32.5)
    // ─────────────────────────────────────────────────────────────────────────

    const val NODES         = "nodes"

    // ─────────────────────────────────────────────────────────────────────────
    // Dashboard / Network feature routes
    // ─────────────────────────────────────────────────────────────────────────

    const val NETWORK_DASHBOARD = "network-dashboard"
    const val EMERGENCY_DASHBOARD = "emergency-dashboard"
    const val CHAT_SCREEN         = "chat-screen/{convId}/{label}"

    fun chatScreen(convId: String, label: String) = "chat-screen/$convId/${label.ifBlank { "Node" }}"

    // ─────────────────────────────────────────────────────────────────────────
    // Profile / Settings routes
    // ─────────────────────────────────────────────────────────────────────────

    const val PROFILE       = "profile"
    const val SETTINGS      = "settings"
    const val ABOUT         = "about"
}
