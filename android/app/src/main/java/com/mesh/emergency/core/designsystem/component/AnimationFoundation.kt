/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mesh.emergency.core.designsystem.theme.MeshThemeTokens

// ─────────────────────────────────────────────────────────────────────────────
// Animation Foundation — Reusable animation specs and composable effects
//
// Keep animations:
//   - Subtle (scale <= 1.15, alpha diffs <= 0.3)
//   - Short duration (200-400ms) to minimize battery impact
//   - Accessible (reducedMotion-aware future hook)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Pulsing ring animation used on active SOS / emergency indicators.
 * Renders an expanding translucent ring that loops indefinitely.
 *
 * @param color Ring color, typically [SemanticColors.emergency].
 * @param size Diameter of the inner solid circle.
 */
@Composable
fun PulsingRing(
    color: Color,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "PulsingRing")

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue  = 1.6f,
        animationSpec = infiniteRepeatable(
            animation  = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseScale"
    )

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue  = 0f,
        animationSpec = infiniteRepeatable(
            animation  = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseAlpha"
    )

    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        // Animated ring
        Box(
            modifier = Modifier
                .size(size)
                .scale(scale)
                .clip(CircleShape)
                .background(color.copy(alpha = alpha))
        )
        // Static inner circle
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(color)
        )
    }
}

/**
 * Network signal ping animation — three expanding concentric rings that
 * stagger outward, indicating active scanning / broadcasting.
 */
@Composable
fun ScanningRipple(
    color: Color = MaterialTheme.colorScheme.primary,
    size: Dp = 32.dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ScanRipple")

    val scale1 by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 1.5f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Restart),
        label = "Ripple1"
    )
    val alpha1 by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Restart),
        label = "Alpha1"
    )
    val scale2 by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 1.5f,
        animationSpec = infiniteRepeatable(tween(1200, delayMillis = 400), RepeatMode.Restart),
        label = "Ripple2"
    )
    val alpha2 by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1200, delayMillis = 400), RepeatMode.Restart),
        label = "Alpha2"
    )

    Box(contentAlignment = Alignment.Center, modifier = modifier.size(size * 2)) {
        Box(
            modifier = Modifier
                .size(size).scale(scale2)
                .clip(CircleShape)
                .background(color.copy(alpha = alpha2))
        )
        Box(
            modifier = Modifier
                .size(size).scale(scale1)
                .clip(CircleShape)
                .background(color.copy(alpha = alpha1))
        )
        Box(
            modifier = Modifier
                .size(size * 0.6f)
                .clip(CircleShape)
                .background(color)
        )
    }
}

/**
 * Shimmer loading placeholder for a text line or image area.
 * Uses a horizontal alpha gradient sweep animation.
 *
 * @param width Width of the placeholder.
 * @param height Height of the placeholder.
 */
@Composable
fun ShimmerPlaceholder(
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier
) {
    val aurora = MeshThemeTokens.auroraColors
    val infiniteTransition = rememberInfiniteTransition(label = "Shimmer")

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue  = 0.7f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "ShimmerAlpha"
    )

    Box(
        modifier = modifier
            .size(width, height)
            .clip(MaterialTheme.shapes.small)
            .background(aurora.glassOverlay.copy(alpha = alpha))
    )
}
