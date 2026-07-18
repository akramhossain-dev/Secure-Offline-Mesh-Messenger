/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.mesh.emergency.core.designsystem.theme.MeshThemeTokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Standard primary action button.
 */
@Composable
fun MeshButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        shape = MaterialTheme.shapes.small
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
            }
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/**
 * Standard outlined button.
 */
@Composable
fun MeshOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = MaterialTheme.shapes.small
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
            }
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/**
 * Minimal text button.
 */
@Composable
fun MeshTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

/**
 * Standard icon button.
 */
@Composable
fun MeshIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint
        )
    }
}

/**
 * Emergency SOS Button requiring a long-press interaction to prevent accidental triggers.
 *
 * Visuals:
 * - Red glowing circle
 * - Centered "SOS" label
 * - Press-and-hold progress feedback
 */
@Composable
fun MeshSosButton(
    onSosTriggered: () -> Unit,
    modifier: Modifier = Modifier,
    holdDurationMs: Long = 1500L
) {
    val spacing = MeshThemeTokens.spacing
    val semanticColors = MeshThemeTokens.semanticColors
    var isPressing by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    val coroutineScope = rememberCoroutineScope()

    val scale by animateFloatAsState(
        targetValue = if (isPressing) 1.15f else 1.0f,
        label = "SosScaleAnimation"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(spacing.sosTouchTarget)
            .scale(scale)
            .clip(CircleShape)
            .background(semanticColors.emergency)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressing = true
                        progress = 0f
                        val startTime = System.currentTimeMillis()
                        val job = coroutineScope.launch {
                            while (isPressing && progress < 1f) {
                                val elapsed = System.currentTimeMillis() - startTime
                                progress = (elapsed.toFloat() / holdDurationMs).coerceIn(0f, 1f)
                                delay(20L)
                            }
                            if (progress >= 1f) {
                                onSosTriggered()
                                isPressing = false
                                progress = 0f
                            }
                        }
                        tryAwaitRelease()
                        isPressing = false
                        job.cancel()
                        progress = 0f
                    }
                )
            }
    ) {
        // Shimmer progress indicator surrounding the SOS text
        if (isPressing) {
            CircularProgressIndicator(
                progress = { progress },
                color = Color.White,
                strokeWidth = 4.dp,
                modifier = Modifier.size(spacing.sosTouchTarget - 4.dp)
            )
        }

        Text(
            text = "SOS",
            color = Color.White,
            style = MaterialTheme.typography.titleLarge
        )
    }
}
