/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────────────────────────────────────
// Spacing System — 4dp base grid
//
// All UI spacing values must be derived from this system to ensure visual
// consistency. Use via `LocalSpacing.current` inside any Composable within
// MeshTheme.
//
// Usage:
// ```kotlin
// val spacing = LocalSpacing.current
// Modifier.padding(horizontal = spacing.md, vertical = spacing.sm)
// ```
// ─────────────────────────────────────────────────────────────────────────────

@Immutable
data class Spacing(
    // ── Base grid values ─────────────────────────────────────────────────────
    val none:   Dp = 0.dp,
    val xxs:    Dp = 2.dp,
    val xs:     Dp = 4.dp,
    val sm:     Dp = 8.dp,
    val md:     Dp = 12.dp,
    val lg:     Dp = 16.dp,
    val xl:     Dp = 20.dp,
    val xxl:    Dp = 24.dp,
    val xxxl:   Dp = 32.dp,
    val huge:   Dp = 40.dp,
    val massive: Dp = 48.dp,
    val giant:  Dp = 64.dp,

    // ── Semantic spacing ──────────────────────────────────────────────────────
    /** Default horizontal screen edge padding. */
    val screenHorizontal:   Dp = 16.dp,

    /** Default vertical screen top/bottom padding. */
    val screenVertical:     Dp = 16.dp,

    /** Standard card internal padding. */
    val cardPadding:        Dp = 16.dp,

    /** Padding between list items. */
    val listItemSpacing:    Dp = 8.dp,

    /** Padding inside a list item. */
    val listItemPadding:    Dp = 12.dp,

    /** Icon size for action bar / navigation icons. */
    val iconSizeMd:         Dp = 24.dp,

    /** Icon size for large feature icons (e.g., SOS button). */
    val iconSizeLg:         Dp = 48.dp,

    /** SOS button minimum touch target (emergency safety). */
    val sosTouchTarget:     Dp = 72.dp,

    /** Bottom navigation bar height. */
    val bottomNavHeight:    Dp = 56.dp,

    /** Top app bar height. */
    val topBarHeight:       Dp = 56.dp,

    /** FAB size. */
    val fabSize:            Dp = 56.dp,

    /** Default divider thickness. */
    val dividerThickness:   Dp = 1.dp,
)
