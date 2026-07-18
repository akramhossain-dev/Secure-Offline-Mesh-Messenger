/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mesh.emergency.core.designsystem.theme.MeshThemeTokens

/**
 * Reusable Badge to indicate unread message counts or alerts.
 */
@Composable
fun MeshBadge(
    count: Int,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.error,
    contentColor: Color = MaterialTheme.colorScheme.onError
) {
    if (count <= 0) return
    val displayStr = if (count > 99) "99+" else count.toString()
    val spacing = MeshThemeTokens.spacing

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .sizeIn(minWidth = 20.dp, minHeight = 20.dp)
            .clip(CircleShape)
            .background(containerColor)
            .padding(horizontal = spacing.xs, vertical = 2.dp)
    ) {
        Text(
            text = displayStr,
            color = contentColor,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
