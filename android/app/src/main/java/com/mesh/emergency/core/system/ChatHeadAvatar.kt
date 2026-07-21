/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.system

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.Icon
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height

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
                modifier           = Modifier.size(32.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text  = "Drag here to remove",
                color = Color.White,
                style = androidx.compose.material3.MaterialTheme.typography.labelMedium
                    .copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            )
        }
    }
}


private val AvatarBlue  = Color(0xFF0084FF)
private val AvatarBlue2 = Color(0xFF0055CC)

/**
 * Circular floating chat head avatar — Messenger-style.
 * Pulsates when [pulseEnabled] is true (new message arrived).
 */
@Composable
fun ChatHeadAvatarCompose(
    label: String,
    unreadCount: Int = 0,
    pulseEnabled: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue   = 1f,
        targetValue    = if (pulseEnabled) 1.12f else 1f,
        animationSpec  = infiniteRepeatable(
            animation  = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = Modifier
            .padding(6.dp)
            .size(62.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer glow ring when pulsing
        if (pulseEnabled) {
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(AvatarBlue.copy(alpha = 0.25f))
            )
        }

        // Main avatar circle with gradient
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(AvatarBlue, AvatarBlue2)
                    )
                )
                .border(2.5.dp, Color.White.copy(alpha = 0.9f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            val display = if (label.startsWith("Global")) "🌐"
                          else label.take(2).uppercase()
            Text(
                text  = display,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color      = Color.White,
                    fontSize   = if (display == "🌐") 22.sp else 17.sp
                )
            )
        }

        // Unread badge
        if (unreadCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 2.dp, y = (-2).dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF3B30))
                    .border(1.5.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text  = if (unreadCount > 99) "99+" else unreadCount.toString(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize   = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color      = Color.White
                    )
                )
            }
        }
    }
}
