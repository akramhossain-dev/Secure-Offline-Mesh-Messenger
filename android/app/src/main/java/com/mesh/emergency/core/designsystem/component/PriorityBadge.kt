/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mesh.emergency.core.designsystem.theme.MeshThemeTokens
import com.mesh.emergency.core.designsystem.theme.Red40
import com.mesh.emergency.core.designsystem.theme.Red80
import com.mesh.emergency.core.designsystem.theme.Amber60
import com.mesh.emergency.core.designsystem.theme.Amber80
import com.mesh.emergency.core.designsystem.theme.Teal40
import com.mesh.emergency.core.designsystem.theme.Teal80
import androidx.compose.foundation.isSystemInDarkTheme

// ─────────────────────────────────────────────────────────────────────────────
// Priority Badge — Emergency alert priority visual indicator
//
// Used to communicate urgency level on:
//   - SOS alerts
//   - Message priority tags
//   - Network event severity indicators
//
// Colours follow the emergency hierarchy defined in SemanticColors.
// ─────────────────────────────────────────────────────────────────────────────

enum class AlertPriorityLevel { CRITICAL, HIGH, NORMAL, LOW }

@Composable
fun PriorityBadge(
    level: AlertPriorityLevel,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true
) {
    val isDark = isSystemInDarkTheme()

    val (label, bgColor, textColor) = when (level) {
        AlertPriorityLevel.CRITICAL -> Triple(
            "CRITICAL",
            if (isDark) Red40.copy(alpha = 0.25f) else Red40.copy(alpha = 0.12f),
            if (isDark) Red80 else Red40
        )
        AlertPriorityLevel.HIGH     -> Triple(
            "HIGH",
            if (isDark) Amber60.copy(alpha = 0.25f) else Amber60.copy(alpha = 0.12f),
            if (isDark) Amber80 else Amber60
        )
        AlertPriorityLevel.NORMAL   -> Triple(
            "NORMAL",
            if (isDark) Teal40.copy(alpha = 0.25f) else Teal40.copy(alpha = 0.12f),
            if (isDark) Teal80 else Teal40
        )
        AlertPriorityLevel.LOW      -> Triple(
            "LOW",
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    if (showLabel) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .clip(RoundedCornerShape(4.dp))
                .background(bgColor)
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = label,
                color = textColor,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}
