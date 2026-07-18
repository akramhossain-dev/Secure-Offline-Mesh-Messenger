/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mesh.emergency.core.designsystem.theme.MeshThemeTokens

// ─────────────────────────────────────────────────────────────────────────────
// Glass Panel — Aurora UI glassmorphism surface
//
// Usage:
//   GlassPanel { /* content */ }
//   GlassPanel(variant = GlassPanelVariant.EMERGENCY) { /* SOS content */ }
//
// The glass effect is achieved using translucent background fills + a subtle
// white/dark border stroke (inner rim highlight). No actual blur shader is
// applied here — blur requires RenderEffect on API 31+ which would be added
// in a future display-layer polish pass.
// ─────────────────────────────────────────────────────────────────────────────

enum class GlassPanelVariant { DEFAULT, EMERGENCY, WARNING }

@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    variant: GlassPanelVariant = GlassPanelVariant.DEFAULT,
    cornerRadius: Dp = 16.dp,
    contentPadding: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val aurora = MeshThemeTokens.auroraColors

    val fillColor = when (variant) {
        GlassPanelVariant.DEFAULT   -> aurora.glassSurface
        GlassPanelVariant.EMERGENCY -> aurora.glassEmergency
        GlassPanelVariant.WARNING   -> aurora.glassWarning
    }

    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .clip(shape)
            .background(fillColor)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        aurora.glassBorder.copy(alpha = 0.4f),
                        aurora.glassBorder.copy(alpha = 0.1f)
                    )
                ),
                shape = shape
            )
            .padding(contentPadding),
        content = content
    )
}

/**
 * Gradient backdrop composable — renders a vertical Aurora gradient fill
 * behind content. Place as the root of any full-screen composable.
 */
@Composable
fun AuroraBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val aurora = MeshThemeTokens.auroraColors

    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    listOf(aurora.auroraEnd, aurora.auroraMid, aurora.auroraStart)
                )
            ),
        content = content
    )
}
