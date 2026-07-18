/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mesh.emergency.core.common.extensions.capitalizeFirst

/**
 * Standard Avatar displaying user/contact initials.
 */
@Composable
fun MeshAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    val initials = name.trim().split("\\s+".toRegex())
        .mapNotNull { it.firstOrNull()?.toString() }
        .take(2)
        .joinToString("")
        .capitalizeFirst()

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(containerColor)
    ) {
        Text(
            text = initials.ifEmpty { "?" },
            color = contentColor,
            style = MaterialTheme.typography.titleMedium
        )
    }
}
