/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
// Mesh Color Palette
// Brand direction: Emergency communications — confident, trustworthy, urgent.
// Primary: Deep Indigo (authority, trust, radio comms aesthetic)
// Secondary: Electric Teal (signal, connectivity)
// Tertiary: Amber (alerts, warnings)
// Error: Emergency Red (SOS, critical alerts)
// ─────────────────────────────────────────────────────────────────────────────

// ── Primary — Deep Indigo ────────────────────────────────────────────────────
val Indigo10   = Color(0xFF0D0C2B)
val Indigo20   = Color(0xFF1A1856)
val Indigo30   = Color(0xFF282481)
val Indigo40   = Color(0xFF3730AD)
val Indigo50   = Color(0xFF4F4BD4)  // Primary brand
val Indigo60   = Color(0xFF7674DC)
val Indigo70   = Color(0xFF9D9BE5)
val Indigo80   = Color(0xFFC4C3EE)
val Indigo90   = Color(0xFFE5E4F8)
val Indigo95   = Color(0xFFF2F1FB)
val Indigo99   = Color(0xFFFCFBFF)

// ── Secondary — Electric Teal ────────────────────────────────────────────────
val Teal10     = Color(0xFF001F21)
val Teal20     = Color(0xFF003A3D)
val Teal30     = Color(0xFF00565B)
val Teal40     = Color(0xFF007178)  // Secondary brand
val Teal50     = Color(0xFF008C96)
val Teal60     = Color(0xFF00A8B4)
val Teal70     = Color(0xFF2AC4D2)
val Teal80     = Color(0xFF5ADFF0)
val Teal90     = Color(0xFFB0F1FA)
val Teal95     = Color(0xFFD8F8FF)
val Teal99     = Color(0xFFF3FDFF)

// ── Tertiary — Amber (Alerts) ────────────────────────────────────────────────
val Amber10    = Color(0xFF1F1300)
val Amber20    = Color(0xFF3B2500)
val Amber30    = Color(0xFF573800)
val Amber40    = Color(0xFF754C00)
val Amber50    = Color(0xFF956100)
val Amber60    = Color(0xFFB77600)
val Amber70    = Color(0xFFDA8E00)
val Amber80    = Color(0xFFFFB82A)  // Tertiary brand
val Amber90    = Color(0xFFFFDDB3)
val Amber95    = Color(0xFFFFEDD6)
val Amber99    = Color(0xFFFFFBFF)

// ── Error — Emergency Red ─────────────────────────────────────────────────────
val Red10      = Color(0xFF410002)
val Red20      = Color(0xFF690005)
val Red30      = Color(0xFF93000A)
val Red40      = Color(0xFFBA1A1A)  // Error / SOS primary
val Red50      = Color(0xFFDE3730)
val Red60      = Color(0xFFFF5449)
val Red70      = Color(0xFFFF897D)
val Red80      = Color(0xFFFFB4AB)
val Red90      = Color(0xFFFFDAD6)
val Red95      = Color(0xFFFFEDEA)
val Red99      = Color(0xFFFFFBFF)

// ── Neutral ───────────────────────────────────────────────────────────────────
val Neutral10  = Color(0xFF1B1B1F)
val Neutral20  = Color(0xFF303034)
val Neutral30  = Color(0xFF47464A)
val Neutral40  = Color(0xFF5E5E62)
val Neutral50  = Color(0xFF777680)
val Neutral60  = Color(0xFF918F9A)
val Neutral70  = Color(0xFFACABB5)
val Neutral80  = Color(0xFFC7C5D0)
val Neutral90  = Color(0xFFE4E1EC)
val Neutral95  = Color(0xFFF2EFFE)
val Neutral99  = Color(0xFFFFFBFF)

// ── Pure Dark Theme Surfaces ──────────────────────────────────────────────────
// Required for the "Pure Black" dark mode (AMOLED-friendly)
val PureBlack     = Color(0xFF000000)  // True black — background
val CardBlack     = Color(0xFF121212)  // Cards / surfaces
val ElevatedBlack = Color(0xFF1E1E1E)  // Elevated surfaces / surface variant

// ── Neutral Variant ───────────────────────────────────────────────────────────
val NeutralVar10  = Color(0xFF1B1B23)
val NeutralVar20  = Color(0xFF303039)
val NeutralVar30  = Color(0xFF464650)
val NeutralVar40  = Color(0xFF5E5E68)
val NeutralVar50  = Color(0xFF767682)
val NeutralVar60  = Color(0xFF90909C)
val NeutralVar70  = Color(0xFFABABB6)
val NeutralVar80  = Color(0xFFC6C5D1)
val NeutralVar90  = Color(0xFFE2E1EE)
val NeutralVar95  = Color(0xFFF0EFFC)
val NeutralVar99  = Color(0xFFFFFBFF)

// ── Aurora Backdrop Accents ────────────────────────────────────────────────────
val AuroraBlueLight = Color(0x1F4F4BD4)
val AuroraTealLight = Color(0x1F008C96)
val AuroraBlueDark  = Color(0x1A7674DC)
val AuroraTealDark  = Color(0x1A5ADFF0)

// ─────────────────────────────────────────────────────────────────────────────
// Material 3 — Light Color Scheme Tokens
// ─────────────────────────────────────────────────────────────────────────────
val md_theme_light_primary              = Indigo50
val md_theme_light_onPrimary            = Color.White
val md_theme_light_primaryContainer     = Indigo90
val md_theme_light_onPrimaryContainer   = Indigo10
val md_theme_light_secondary            = Teal40
val md_theme_light_onSecondary          = Color.White
val md_theme_light_secondaryContainer   = Teal90
val md_theme_light_onSecondaryContainer = Teal10
val md_theme_light_tertiary             = Amber80
val md_theme_light_onTertiary           = Amber10
val md_theme_light_tertiaryContainer    = Amber90
val md_theme_light_onTertiaryContainer  = Amber10
val md_theme_light_error                = Red40
val md_theme_light_errorContainer       = Red90
val md_theme_light_onError              = Color.White
val md_theme_light_onErrorContainer     = Red10
val md_theme_light_background           = Neutral99
val md_theme_light_onBackground         = Neutral10
val md_theme_light_surface              = Neutral99
val md_theme_light_onSurface            = Neutral10
val md_theme_light_surfaceVariant       = NeutralVar90
val md_theme_light_onSurfaceVariant     = NeutralVar30
val md_theme_light_outline              = NeutralVar50
val md_theme_light_inverseOnSurface     = Neutral95
val md_theme_light_inverseSurface       = Neutral20
val md_theme_light_inversePrimary       = Indigo80
val md_theme_light_shadow               = Neutral10
val md_theme_light_surfaceTint          = Indigo50
val md_theme_light_outlineVariant       = NeutralVar80
val md_theme_light_scrim                = Neutral10

// ─────────────────────────────────────────────────────────────────────────────
// Material 3 — Dark Color Scheme Tokens
// ─────────────────────────────────────────────────────────────────────────────
val md_theme_dark_primary               = Indigo80
val md_theme_dark_onPrimary             = Indigo20
val md_theme_dark_primaryContainer      = Indigo30
val md_theme_dark_onPrimaryContainer    = Indigo90
val md_theme_dark_secondary             = Teal80
val md_theme_dark_onSecondary           = Teal20
val md_theme_dark_secondaryContainer    = Teal30
val md_theme_dark_onSecondaryContainer  = Teal90
val md_theme_dark_tertiary              = Amber80
val md_theme_dark_onTertiary            = Amber20
val md_theme_dark_tertiaryContainer     = Amber30
val md_theme_dark_onTertiaryContainer   = Amber90
val md_theme_dark_error                 = Red80
val md_theme_dark_errorContainer        = Red30
val md_theme_dark_onError               = Red20
val md_theme_dark_onErrorContainer      = Red90
val md_theme_dark_background            = PureBlack      // #000000 — AMOLED pure black
val md_theme_dark_onBackground          = Neutral90
val md_theme_dark_surface               = CardBlack      // #121212 — card surfaces
val md_theme_dark_onSurface             = Neutral90
val md_theme_dark_surfaceVariant        = ElevatedBlack  // #1E1E1E — elevated surfaces
val md_theme_dark_onSurfaceVariant      = NeutralVar80
val md_theme_dark_outline               = NeutralVar60
val md_theme_dark_inverseOnSurface      = Neutral10
val md_theme_dark_inverseSurface        = Neutral90
val md_theme_dark_inversePrimary        = Indigo50
val md_theme_dark_shadow                = Neutral10
val md_theme_dark_surfaceTint           = Indigo80
val md_theme_dark_outlineVariant        = NeutralVar30
val md_theme_dark_scrim                 = Neutral10

// ─────────────────────────────────────────────────────────────────────────────
// Semantic / Feature-specific color tokens (M3 scheme adjacent)
// ─────────────────────────────────────────────────────────────────────────────
@Immutable
data class SemanticColors(
    val connected: Color,
    val offline: Color,
    val weakSignal: Color,
    val strongSignal: Color,
    val emergency: Color,
    val warning: Color,
    val success: Color,
    val info: Color,
    val disabled: Color,
    val outline: Color,
    val messageSent: Color,
    val messageDelivered: Color,
    val messageFailed: Color
)

val LightSemanticColors = SemanticColors(
    connected = Teal50,
    offline = Neutral50,
    weakSignal = Amber60,
    strongSignal = Teal60,
    emergency = Red50,
    warning = Amber70,
    success = Teal60,
    info = Indigo50,
    disabled = Neutral80,
    outline = NeutralVar50,
    messageSent = Neutral50,
    messageDelivered = Teal50,
    messageFailed = Red40
)

val DarkSemanticColors = SemanticColors(
    connected = Teal60,
    offline = Neutral60,
    weakSignal = Amber50,
    strongSignal = Teal70,
    emergency = Red60,
    warning = Amber60,
    success = Teal70,
    info = Indigo60,
    disabled = Neutral40,
    outline = NeutralVar60,
    messageSent = Neutral60,
    messageDelivered = Teal60,
    messageFailed = Red70
)
