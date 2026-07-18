/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A beautiful custom widget indicating cellular or Bluetooth signal strength.
 * Renders four vertical signal bars whose heights scale progressively.
 */
@Composable
fun SignalStrengthBars(
    rssi: Int,
    modifier: Modifier = Modifier
) {
    val activeBars = when {
        rssi >= -60 -> 4
        rssi >= -70 -> 3
        rssi >= -85 -> 2
        rssi >= -95 -> 1
        else -> 0
    }

    Row(
        modifier = modifier.height(16.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        for (i in 1..4) {
            val barHeight = (i * 4).dp
            val isActive = i <= activeBars
            val color = if (isActive) {
                when (activeBars) {
                    1 -> MaterialTheme.colorScheme.error
                    2 -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.primary
                }
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
            }
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(barHeight)
                    .background(color = color)
            )
        }
    }
}
