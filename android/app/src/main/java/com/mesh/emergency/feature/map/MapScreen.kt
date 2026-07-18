/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.feature.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mesh.emergency.core.designsystem.component.AuroraBackdrop
import com.mesh.emergency.core.designsystem.component.GlassPanel
import com.mesh.emergency.core.designsystem.theme.MeshThemeTokens
import com.mesh.emergency.core.map.MapLayerModel
import com.mesh.emergency.core.map.MapLayerType
import com.mesh.emergency.core.map.MapState
import com.mesh.emergency.domain.repository.NodeDomainModel

/**
 * Offline Map Screen — renders a canvas-based grid map with node pins and layer toggles.
 *
 * No online tile source is used. Simulates geographic context for offline emergency use.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onBack: () -> Unit = {},
    viewModel: MapViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val mapState by viewModel.mapState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val spacing = MeshThemeTokens.spacing

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    AuroraBackdrop(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "Offline Map",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                when (mapState) {
                                    MapState.LOADED  -> "Ready • ${uiState.visibleNodes.size} nodes"
                                    MapState.LOADING -> "Loading map data…"
                                    MapState.ERROR   -> "Map error"
                                    MapState.EMPTY   -> "No map data"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = when (mapState) {
                                    MapState.LOADED  -> MeshThemeTokens.semanticColors.connected
                                    MapState.ERROR   -> MeshThemeTokens.semanticColors.emergency
                                    else             -> MeshThemeTokens.semanticColors.offline
                                }
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            floatingActionButton = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FloatingActionButton(
                        onClick = { viewModel.onZoomIn() },
                        containerColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Zoom in", modifier = Modifier.size(20.dp))
                    }
                    FloatingActionButton(
                        onClick = { viewModel.onZoomOut() },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Zoom out", modifier = Modifier.size(20.dp))
                    }
                }
            }
        ) { paddingValues ->
            LazyColumn(
                contentPadding = PaddingValues(
                    start = spacing.lg, end = spacing.lg,
                    top = paddingValues.calculateTopPadding() + spacing.xs,
                    bottom = paddingValues.calculateBottomPadding() + spacing.lg
                ),
                verticalArrangement = Arrangement.spacedBy(spacing.md),
                modifier = Modifier.fillMaxSize()
            ) {

                // ── Layer toggle chips ─────────────────────────────────────────
                item {
                    LayerToggleRow(
                        layers = uiState.layers,
                        onToggle = { id, vis -> viewModel.onLayerToggled(id, vis) }
                    )
                }

                // ── Map canvas ────────────────────────────────────────────────
                item {
                    OfflineMapCanvas(
                        nodes = uiState.visibleNodes,
                        zoomLevel = uiState.zoomLevel,
                        layers = uiState.layers,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(360.dp)
                    )
                }

                // ── Zoom level indicator ───────────────────────────────────────
                item {
                    GlassPanel(modifier = Modifier.fillMaxWidth(), contentPadding = 10.dp) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Zoom Level",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "${uiState.zoomLevel}x",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // ── Node pins list ────────────────────────────────────────────
                item {
                    Text(
                        "Pinned Nodes (${uiState.visibleNodes.size})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (uiState.visibleNodes.isEmpty()) {
                    item {
                        GlassPanel(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "No nodes with location data discovered yet.\nNodes will appear once mesh contact is made.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                items(uiState.visibleNodes, key = { it.id }) { node ->
                    NodePinRow(node = node)
                }

                // ── Offline disclaimer ─────────────────────────────────────────
                item {
                    GlassPanel(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MeshThemeTokens.semanticColors.warning,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Offline Mode — Grid map only. Real geographic tiles require local map files.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LayerToggleRow(
    layers: List<MapLayerModel>,
    onToggle: (String, Boolean) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 2.dp)
    ) {
        items(layers, key = { it.id }) { layer ->
            FilterChip(
                selected = layer.isVisible,
                onClick = { onToggle(layer.id, !layer.isVisible) },
                label = { Text(layer.name, style = MaterialTheme.typography.labelSmall) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}

@Composable
private fun OfflineMapCanvas(
    nodes: List<NodeDomainModel>,
    zoomLevel: Int,
    layers: List<MapLayerModel>,
    modifier: Modifier = Modifier
) {
    val semanticColors = MeshThemeTokens.semanticColors
    val showNodes = layers.any { it.id == "nodes" && it.isVisible }
    val showRoads = layers.any { it.id == "roads" && it.isVisible }
    val showEmergency = layers.any { it.id == "emergency" && it.isVisible }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0D1B2A))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.5f, 3f)
                    offset = offset + pan
                }
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            }
    ) {
        val gridColor = Color.White.copy(alpha = 0.08f)
        val roadColor = Color(0xFF2196F3).copy(alpha = 0.4f)
        val emergencyColor = Color(0xFFFF5722).copy(alpha = 0.15f)
        val nodeColor = semanticColors.connected

        // Draw base grid
        drawGrid(gridColor)

        // Draw roads layer
        if (showRoads) {
            drawRoads(roadColor)
        }

        // Draw emergency zone
        if (showEmergency) {
            drawEmergencyZone(emergencyColor)
        }

        // Draw node pins
        if (showNodes) {
            nodes.forEachIndexed { idx, _ ->
                val x = size.width * (0.2f + (idx * 0.15f) % 0.6f)
                val y = size.height * (0.3f + (idx * 0.12f) % 0.4f)
                drawCircle(color = nodeColor, radius = 6.dp.toPx(), center = Offset(x, y))
                drawCircle(color = nodeColor.copy(alpha = 0.3f), radius = 12.dp.toPx(), center = Offset(x, y))
            }
        }

        // Draw "you are here" marker
        val cx = size.width * 0.5f
        val cy = size.height * 0.5f
        drawCircle(color = Color(0xFFFFEB3B), radius = 8.dp.toPx(), center = Offset(cx, cy))
        drawCircle(color = Color.White, radius = 4.dp.toPx(), center = Offset(cx, cy))
    }
}

private fun DrawScope.drawGrid(color: Color) {
    val step = 40.dp.toPx()
    var x = 0f
    while (x <= size.width) {
        drawLine(color = color, start = Offset(x, 0f), end = Offset(x, size.height), strokeWidth = 1f)
        x += step
    }
    var y = 0f
    while (y <= size.height) {
        drawLine(color = color, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 1f)
        y += step
    }
}

private fun DrawScope.drawRoads(color: Color) {
    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 10f))
    drawLine(color = color, start = Offset(0f, size.height * 0.5f), end = Offset(size.width, size.height * 0.5f), strokeWidth = 3f)
    drawLine(color = color, start = Offset(size.width * 0.33f, 0f), end = Offset(size.width * 0.33f, size.height), strokeWidth = 2f, pathEffect = dashEffect)
    drawLine(color = color, start = Offset(size.width * 0.66f, 0f), end = Offset(size.width * 0.66f, size.height), strokeWidth = 2f, pathEffect = dashEffect)
}

private fun DrawScope.drawEmergencyZone(color: Color) {
    drawCircle(color = color, radius = size.minDimension * 0.25f, center = Offset(size.width * 0.5f, size.height * 0.5f))
}

@Composable
private fun NodePinRow(node: NodeDomainModel) {
    GlassPanel(modifier = Modifier.fillMaxWidth(), contentPadding = 10.dp) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MeshThemeTokens.semanticColors.connected)
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        node.deviceId.take(12),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Lat: %.4f  Lon: %.4f".format(node.latitude, node.longitude),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                node.status,
                style = MaterialTheme.typography.labelSmall,
                color = MeshThemeTokens.semanticColors.info
            )
        }
    }
}
