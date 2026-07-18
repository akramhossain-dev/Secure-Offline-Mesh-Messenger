/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mesh.emergency.core.designsystem.theme.MeshThemeTokens

/**
 * Standard Minimal Flat M3 Card.
 */
@Composable
fun MeshCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    content: @Composable ColumnScope.() -> Unit
) {
    val spacing = MeshThemeTokens.spacing
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(spacing.cardPadding),
            content = content
        )
    }
}

/**
 * Custom Glassmorphic Card providing translucent glass aesthetics.
 * Uses a thin white/grey gradient border and semi-transparent overlay.
 */
@Composable
fun MeshGlassCard(
    modifier: Modifier = Modifier,
    borderColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
    content: @Composable ColumnScope.() -> Unit
) {
    val spacing = MeshThemeTokens.spacing
    val shape = MaterialTheme.shapes.medium

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)
            )
            .border(
                border = BorderStroke(
                    width = 1.dp,
                    color = borderColor
                ),
                shape = shape
            )
    ) {
        Column(
            modifier = Modifier.padding(spacing.cardPadding),
            content = content
        )
    }
}
