/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.feature.resources

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mesh.emergency.core.designsystem.component.GlassPanel
import com.mesh.emergency.core.designsystem.theme.MeshThemeTokens
import com.mesh.emergency.domain.repository.ResourceDomainModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Bottom sheet showing resource details with action controls.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourceDetailSheet(
    resource: ResourceDomainModel,
    onDismiss: () -> Unit,
    onMarkUnavailable: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartialExpansion = true)
    val spacing = MeshThemeTokens.spacing
    val category = ResourceCategory.entries.firstOrNull { it.key == resource.type }
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.lg, vertical = spacing.md)
        ) {
            // ── Header ─────────────────────────────────────────────────────────
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "${category?.emoji ?: "📦"} ${resource.name}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(Modifier.height(spacing.md))

            // ── Details ────────────────────────────────────────────────────────
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailRow("Category", category?.label ?: resource.type)
                    DetailRow("Quantity", resource.quantity.toString())
                    DetailRow("Status", resource.status)
                    DetailRow("Privacy", resource.privacy)
                    if (resource.description.isNotBlank()) {
                        DetailRow("Notes", resource.description)
                    }
                    DetailRow("TTL Expires", dateFormat.format(Date(resource.ttl)))
                    if (resource.latitude != 0.0 || resource.longitude != 0.0) {
                        DetailRow("Location", "%.4f, %.4f".format(resource.latitude, resource.longitude))
                    }
                }
            }

            Spacer(Modifier.height(spacing.md))

            // ── Actions ────────────────────────────────────────────────────────
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onMarkUnavailable,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MeshThemeTokens.semanticColors.warning
                    )
                ) {
                    Text("Mark Unavailable", style = MaterialTheme.typography.labelMedium)
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Close")
                }
            }

            Spacer(Modifier.height(spacing.lg))
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
