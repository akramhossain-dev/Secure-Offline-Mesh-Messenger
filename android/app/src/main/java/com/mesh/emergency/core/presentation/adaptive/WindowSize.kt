/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.presentation.adaptive

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

/**
 * Classifies screen display dimensions for responsive design calculations.
 */
enum class WindowSize {
    /** Mobile phones in portrait mode. */
    COMPACT,

    /** Tablets in portrait / foldables half-open / large phones. */
    MEDIUM,

    /** Large tablets and foldables in landscape. */
    EXPANDED
}

/**
 * Computes the local screen dimensions classification.
 */
@Composable
fun rememberWindowSize(): WindowSize {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    return when {
        screenWidth < 600.dp -> WindowSize.COMPACT
        screenWidth < 840.dp -> WindowSize.MEDIUM
        else                 -> WindowSize.EXPANDED
    }
}
