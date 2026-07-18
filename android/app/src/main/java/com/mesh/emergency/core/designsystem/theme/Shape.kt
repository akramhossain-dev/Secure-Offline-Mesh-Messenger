/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────────────────────────────────────
// Shape System — Material Design 3
//
// Shape values chosen for an emergency/tactical aesthetic:
// - ExtraSmall: chips, badges, input corners (tight, precise)
// - Small: buttons, cards (slightly rounded)
// - Medium: dialogs, bottom sheets (comfortable)
// - Large: navigation drawers, expanded content
// - ExtraLarge: full-screen sheets, modal surfaces
// ─────────────────────────────────────────────────────────────────────────────

val MeshShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small      = RoundedCornerShape(8.dp),
    medium     = RoundedCornerShape(12.dp),
    large      = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)
