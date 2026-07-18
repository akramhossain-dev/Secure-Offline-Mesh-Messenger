/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

// ─────────────────────────────────────────────────────────────────────────────
// Color Schemes
// ─────────────────────────────────────────────────────────────────────────────

private val LightColorScheme: ColorScheme = lightColorScheme(
    primary              = md_theme_light_primary,
    onPrimary            = md_theme_light_onPrimary,
    primaryContainer     = md_theme_light_primaryContainer,
    onPrimaryContainer   = md_theme_light_onPrimaryContainer,
    secondary            = md_theme_light_secondary,
    onSecondary          = md_theme_light_onSecondary,
    secondaryContainer   = md_theme_light_secondaryContainer,
    onSecondaryContainer = md_theme_light_onSecondaryContainer,
    tertiary             = md_theme_light_tertiary,
    onTertiary           = md_theme_light_onTertiary,
    tertiaryContainer    = md_theme_light_tertiaryContainer,
    onTertiaryContainer  = md_theme_light_onTertiaryContainer,
    error                = md_theme_light_error,
    errorContainer       = md_theme_light_errorContainer,
    onError              = md_theme_light_onError,
    onErrorContainer     = md_theme_light_onErrorContainer,
    background           = md_theme_light_background,
    onBackground         = md_theme_light_onBackground,
    surface              = md_theme_light_surface,
    onSurface            = md_theme_light_onSurface,
    surfaceVariant       = md_theme_light_surfaceVariant,
    onSurfaceVariant     = md_theme_light_onSurfaceVariant,
    outline              = md_theme_light_outline,
    inverseOnSurface     = md_theme_light_inverseOnSurface,
    inverseSurface       = md_theme_light_inverseSurface,
    inversePrimary       = md_theme_light_inversePrimary,
    surfaceTint          = md_theme_light_surfaceTint,
    outlineVariant       = md_theme_light_outlineVariant,
    scrim                = md_theme_light_scrim,
)

private val DarkColorScheme: ColorScheme = darkColorScheme(
    primary              = md_theme_dark_primary,
    onPrimary            = md_theme_dark_onPrimary,
    primaryContainer     = md_theme_dark_primaryContainer,
    onPrimaryContainer   = md_theme_dark_onPrimaryContainer,
    secondary            = md_theme_dark_secondary,
    onSecondary          = md_theme_dark_onSecondary,
    secondaryContainer   = md_theme_dark_secondaryContainer,
    onSecondaryContainer = md_theme_dark_onSecondaryContainer,
    tertiary             = md_theme_dark_tertiary,
    onTertiary           = md_theme_dark_onTertiary,
    tertiaryContainer    = md_theme_dark_tertiaryContainer,
    onTertiaryContainer  = md_theme_dark_onTertiaryContainer,
    error                = md_theme_dark_error,
    errorContainer       = md_theme_dark_errorContainer,
    onError              = md_theme_dark_onError,
    onErrorContainer     = md_theme_dark_onErrorContainer,
    background           = md_theme_dark_background,
    onBackground         = md_theme_dark_onBackground,
    surface              = md_theme_dark_surface,
    onSurface            = md_theme_dark_onSurface,
    surfaceVariant       = md_theme_dark_surfaceVariant,
    onSurfaceVariant     = md_theme_dark_onSurfaceVariant,
    outline              = md_theme_dark_outline,
    inverseOnSurface     = md_theme_dark_inverseOnSurface,
    inverseSurface       = md_theme_dark_inverseSurface,
    inversePrimary       = md_theme_dark_inversePrimary,
    surfaceTint          = md_theme_dark_surfaceTint,
    outlineVariant       = md_theme_dark_outlineVariant,
    scrim                = md_theme_dark_scrim,
)

// ─────────────────────────────────────────────────────────────────────────────
// CompositionLocals — for Spacing and extra theme tokens
// ─────────────────────────────────────────────────────────────────────────────

val LocalSpacing = staticCompositionLocalOf { Spacing() }

// ─────────────────────────────────────────────────────────────────────────────
// MeshTheme — Main composable
// ─────────────────────────────────────────────────────────────────────────────

/**
 * The root theme composable for the Offline Emergency Mesh Communication System.
 *
 * @param themeMode Explicit theme mode; defaults to [ThemeMode.SYSTEM].
 * @param dynamicColor If `true` and running on Android 12+, uses wallpaper-derived
 *                     dynamic colors. Overrides [themeMode] color selection.
 * @param content The composable content to theme.
 */
@Composable
fun MeshTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT  -> false
        ThemeMode.DARK   -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme: ColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }

    CompositionLocalProvider(
        LocalSpacing provides Spacing(),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = MeshTypography,
            shapes      = MeshShapes,
            content     = content,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Convenience accessors
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Provides access to the Mesh spacing system from within a [MeshTheme] context.
 */
object MeshThemeTokens {
    val spacing: Spacing
        @Composable get() = LocalSpacing.current
}
