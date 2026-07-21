/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.overlay

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Red drop zone shown at the bottom of the screen while user drags a chat head.
 * Signals that releasing the circle here will permanently dismiss it.
 */
@Composable
fun TrashDropZone() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xCCB71C1C))
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector        = Icons.Default.DeleteForever,
                contentDescription = "Remove chat head",
                tint               = Color.White,
                modifier           = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text          = "Drag here to remove",
                color         = Color.White,
                fontSize      = 12.sp,
                fontWeight    = FontWeight.Medium
            )
        }
    }
}

/**
 * Messenger-style circular avatar rendered inside a floating ComposeView window.
 */
@Composable
fun ChatHeadAvatar(
    convId: String,
    label: String,
    unreadCount: Int,
    isPulsing: Boolean
) {
    val scaleFactor by if (isPulsing) {
        val transition = rememberInfiniteTransition(label = "pulse")
        transition.animateFloat(
            initialValue = 1.0f,
            targetValue  = 1.14f,
            animationSpec = infiniteRepeatable(
                animation   = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                repeatMode  = RepeatMode.Reverse
            ),
            label = "pulse_scale"
        )
    } else {
        androidx.compose.runtime.rememberUpdatedState(1.0f)
    }

    val isGlobal = convId == "global"

    val backgroundBrush = if (isGlobal) {
        Brush.radialGradient(
            colors = listOf(Color(0xFFFF9800), Color(0xFFE65100))
        )
    } else {
        Brush.radialGradient(
            colors = listOf(Color(0xFF0084FF), Color(0xFF0055CC))
        )
    }

    Box(
        modifier = Modifier
            .size(64.dp)
            .scale(scaleFactor),
        contentAlignment = Alignment.TopEnd
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .align(Alignment.Center)
                .clip(CircleShape)
                .background(backgroundBrush)
                .border(2.5.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (isGlobal) {
                Text(
                    text       = "🌐",
                    fontSize   = 24.sp
                )
            } else {
                val initials = label.trim().take(2).uppercase().ifEmpty { "EC" }
                Text(
                    text       = initials,
                    color      = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 18.sp
                )
            }
        }

        if (unreadCount > 0) {
            Box(
                modifier = Modifier
                    .offset(x = (-2).dp, y = 2.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF3B30))
                    .border(1.5.dp, Color.White, CircleShape)
                    .padding(horizontal = 5.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = if (unreadCount > 99) "99+" else "$unreadCount",
                    color      = Color.White,
                    fontSize   = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
