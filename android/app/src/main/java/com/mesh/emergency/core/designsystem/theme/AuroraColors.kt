/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
// Aurora Glass Token System
//
// Glassmorphism surface tokens for the "Aurora UI" design language.
// All glass layers use translucent overlays on top of rich gradient
// backgrounds. Surfaces should NOT use fully opaque containers — instead
// they borrow the Aurora blur aesthetic using these alpha-controlled tokens.
//
// Usage:
//   - GlassPanel backgrounds → glassBackground
//   - Card inner fill → glassSurface
//   - Emergency glass tint → glassEmergency
//   - Active overlay (shimmer) → glassOverlay
//
// These are SEPARATE from Material3 ColorScheme roles and are provided
// via LocalAuroraColors composition local.
// ─────────────────────────────────────────────────────────────────────────────

@Immutable
data class AuroraColors(
    /** Primary glass panel background — translucent indigo/dark surface. */
    val glassBackground: Color,

    /** Inner glass surface fill — lighter translucent layer. */
    val glassSurface: Color,

    /** Glass tint used on emergency/SOS panels. */
    val glassEmergency: Color,

    /** Glass tint used on warning panels (battery / network). */
    val glassWarning: Color,

    /** Overlay shimmer for loading/active states. */
    val glassOverlay: Color,

    /** Glass border stroke (inner rim highlight). */
    val glassBorder: Color,

    /** Aurora gradient start color (north pole blue). */
    val auroraStart: Color,

    /** Aurora gradient mid color (teal green). */
    val auroraMid: Color,

    /** Aurora gradient end color (deep indigo). */
    val auroraEnd: Color,
)

// ── Light Aurora ──────────────────────────────────────────────────────────────
val LightAuroraColors = AuroraColors(
    glassBackground  = Color(0xCCF2F1FB),   // Indigo95 @ 80% opacity
    glassSurface     = Color(0xE6FFFFFF),    // White @ 90% opacity
    glassEmergency   = Color(0x26BA1A1A),   // Red40 @ 15% opacity
    glassWarning     = Color(0x26FFB82A),   // Amber80 @ 15% opacity
    glassOverlay     = Color(0x1A4F4BD4),   // Indigo50 @ 10% opacity
    glassBorder      = Color(0x33000000),   // Black @ 20% opacity
    auroraStart      = Color(0xFF9D9BE5),   // Indigo70
    auroraMid        = Color(0xFF5ADFF0),   // Teal80
    auroraEnd        = Color(0xFF4F4BD4),   // Indigo50
)

// ── Dark Aurora ───────────────────────────────────────────────────────────────
val DarkAuroraColors = AuroraColors(
    glassBackground  = Color(0xCC0D0C2B),   // Indigo10 @ 80% opacity
    glassSurface     = Color(0x1AFFFFFF),   // White @ 10% opacity
    glassEmergency   = Color(0x33FF5449),   // Red60 @ 20% opacity
    glassWarning     = Color(0x33DA8E00),   // Amber70 @ 20% opacity
    glassOverlay     = Color(0x1A7674DC),   // Indigo60 @ 10% opacity
    glassBorder      = Color(0x33FFFFFF),   // White @ 20% opacity
    auroraStart      = Color(0xFF282481),   // Indigo30
    auroraMid        = Color(0xFF00565B),   // Teal30
    auroraEnd        = Color(0xFF1A1856),   // Indigo20
)

val LocalAuroraColors = androidx.compose.runtime.staticCompositionLocalOf { DarkAuroraColors }
