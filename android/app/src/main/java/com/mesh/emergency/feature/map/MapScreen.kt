/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.feature.map

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mesh.emergency.R
import com.mesh.emergency.core.common.extensions.hasPermission
import com.mesh.emergency.core.designsystem.component.AuroraBackdrop
import com.mesh.emergency.core.designsystem.component.GlassPanel
import com.mesh.emergency.core.designsystem.component.MeshEmptyState
import com.mesh.emergency.core.designsystem.component.PermissionRequiredState
import com.mesh.emergency.core.designsystem.theme.MeshThemeTokens
import com.mesh.emergency.core.designsystem.theme.ThemeMode
import com.mesh.emergency.core.map.MapLayerModel
import com.mesh.emergency.core.map.MapState
import com.mesh.emergency.core.utils.LocationData
import com.mesh.emergency.domain.repository.NodeDomainModel
import com.mesh.emergency.feature.emergency.domain.EmergencyEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Redesigned Offline Map Screen.
 * Implements runtime permission flows and coordinates plotting relative to the active GPS fix.
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
    val context = LocalContext.current

    // Permission handling state
    var hasLocationPermission by remember {
        mutableStateOf(context.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    AuroraBackdrop(modifier = Modifier.fillMaxSize()) {
        if (!hasLocationPermission) {
            PermissionRequiredState(
                permissionName = "Location",
                onRequestPermission = {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Scaffold(
                containerColor = Color.Transparent,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    "Offline Map Dashboard",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    when (mapState) {
                                        MapState.LOADED  -> "System Ready • Active GPS tracking"
                                        MapState.LOADING -> "Initializing offline layers…"
                                        MapState.ERROR   -> "Map framework failure"
                                        MapState.EMPTY   -> "No map tiles loaded"
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

                    // ── 1. Layer Filters ───────────────────────────────────────
                    item {
                        LayerToggleRow(
                            layers = uiState.layers,
                            onToggle = { id, vis -> viewModel.onLayerToggled(id, vis) }
                        )
                    }

                    // ── 2. Canvas & Empty State ────────────────────────────────
                    if (uiState.currentLocation == null && uiState.visibleNodes.isEmpty() && uiState.emergencyEvents.isEmpty()) {
                        item {
                            GlassPanel(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(360.dp),
                                contentPadding = 24.dp
                            ) {
                                MeshEmptyState(
                                    title = "No Map Data Coordinates",
                                    description = "GPS lock pending. Coordinates from discovered LoRa mesh nodes or active SOS alerts will plot automatically relative to your position.",
                                    icon = Icons.Default.MyLocation,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    } else {
                        item {
                            OfflineMapCanvas(
                                currentLocation = uiState.currentLocation,
                                savedLocations = uiState.savedLocations,
                                sharedLocations = uiState.sharedLocations,
                                emergencyEvents = uiState.emergencyEvents,
                                nodes = uiState.visibleNodes,
                                zoomLevel = uiState.zoomLevel,
                                layers = uiState.layers,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(360.dp)
                            )
                        }
                    }

                    // ── 3. Legend Section ──────────────────────────────────────
                    item {
                        MapLegendCard()
                    }

                    // ── 4. Nodes Lists ─────────────────────────────────────────
                    item {
                        Text(
                            "Network Node Telemetries (${uiState.visibleNodes.size})",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (uiState.visibleNodes.isEmpty()) {
                        item {
                            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    "No routing nodes with coordinates located. Once a partner node shares telemetry, it will render above.",
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

                    // ── 5. Location Tracking Status ────────────────────────────
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
                                val gpsFix = if (uiState.currentLocation != null) {
                                    "GPS Fix Active: %.4f, %.4f".format(uiState.currentLocation!!.latitude, uiState.currentLocation!!.longitude)
                                } else {
                                    "Searching for GPS satellites..."
                                }
                                Text(
                                    text = gpsFix,
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
    currentLocation: LocationData?,
    savedLocations: List<LocationData>,
    sharedLocations: List<LocationData>,
    emergencyEvents: List<EmergencyEvent>,
    nodes: List<NodeDomainModel>,
    zoomLevel: Int,
    layers: List<MapLayerModel>,
    modifier: Modifier = Modifier
) {
    val semanticColors = MeshThemeTokens.semanticColors
    
    // Evaluate layer visibilities
    val showBase = layers.any { it.id == "base" && it.isVisible }
    val showRoads = layers.any { it.id == "roads" && it.isVisible }
    val showNodes = layers.any { it.id == "nodes" && it.isVisible }
    val showEmergency = layers.any { it.id == "emergency" && it.isVisible }
    val showElevation = layers.any { it.id == "elevation" && it.isVisible }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0F172A)) // Sleek dark blueprint background
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.5f, 4f)
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
        // Center position defaults to Dhaka coordinates if GPS lock has not occurred yet
        val centerLat = currentLocation?.latitude ?: 23.8103
        val centerLon = currentLocation?.longitude ?: 90.4125

        // Degree coordinates-to-pixels scale mapping: zoom makes coordinates expand
        val baseScale = (size.minDimension / 2f) * (zoomLevel.toFloat() / 10f) * 12000f

        // Draw blueprint grid lines
        if (showBase) {
            drawGrid(Color.White.copy(alpha = 0.05f))
        }

        // Draw simulated highway lines
        if (showRoads) {
            drawRoads(Color(0xFF38BDF8).copy(alpha = 0.25f))
        }

        // Draw local elevation/contour circles
        if (showElevation) {
            drawContourCircles(Color(0xFFE2E8F0).copy(alpha = 0.1f))
        }

        // ── 1. Draw saved local path history (Blue trail dots) ─────────────
        savedLocations.forEach { loc ->
            val dx = (loc.longitude - centerLon).toFloat()
            val dy = (centerLat - loc.latitude).toFloat()
            val px = size.width / 2f + dx * baseScale
            val py = size.height / 2f + dy * baseScale
            
            drawCircle(
                color = Color(0xFF3B82F6).copy(alpha = 0.4f),
                radius = 3.dp.toPx(),
                center = Offset(px, py)
            )
        }

        // ── 2. Draw shared locations of contacts (Green dots) ──────────────
        sharedLocations.forEach { loc ->
            val dx = (loc.longitude - centerLon).toFloat()
            val dy = (centerLat - loc.latitude).toFloat()
            val px = size.width / 2f + dx * baseScale
            val py = size.height / 2f + dy * baseScale

            drawCircle(color = Color(0xFF10B981), radius = 6.dp.toPx(), center = Offset(px, py))
            drawCircle(color = Color(0xFF10B981).copy(alpha = 0.25f), radius = 10.dp.toPx(), center = Offset(px, py))
        }

        // ── 3. Draw active LoRa mesh router nodes (Purple range rings) ──────
        if (showNodes) {
            nodes.forEach { node ->
                val dx = (node.longitude - centerLon).toFloat()
                val dy = (centerLat - node.latitude).toFloat()
                val px = size.width / 2f + dx * baseScale
                val py = size.height / 2f + dy * baseScale

                drawCircle(color = Color(0xFFA855F7), radius = 7.dp.toPx(), center = Offset(px, py))
                // Signal radius overlay
                drawCircle(
                    color = Color(0xFFA855F7).copy(alpha = 0.1f),
                    radius = 24.dp.toPx(),
                    center = Offset(px, py)
                )
            }
        }

        // ── 4. Draw active distress SOS alerts (Blinking red warning circles)
        if (showEmergency) {
            emergencyEvents.forEach { event ->
                if (!event.isResolved) {
                    val dx = (event.longitude - centerLon).toFloat()
                    val dy = (centerLat - event.latitude).toFloat()
                    val px = size.width / 2f + dx * baseScale
                    val py = size.height / 2f + dy * baseScale

                    drawCircle(color = Color(0xFFEF4444), radius = 8.dp.toPx(), center = Offset(px, py))
                    drawCircle(
                        color = Color(0xFFEF4444).copy(alpha = 0.25f),
                        radius = 18.dp.toPx(),
                        center = Offset(px, py)
                    )
                }
            }
        }

        // ── 5. Draw local user position marker (Yellow compass beacon) ──────
        if (currentLocation != null) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            drawCircle(color = Color(0xFFFACC15), radius = 8.dp.toPx(), center = Offset(cx, cy))
            drawCircle(color = Color.White, radius = 3.dp.toPx(), center = Offset(cx, cy))
            drawCircle(
                color = Color(0xFFFACC15).copy(alpha = 0.2f),
                radius = 16.dp.toPx(),
                center = Offset(cx, cy)
            )
        }
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
    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(30f, 15f))
    // Diagonal highway corridors
    drawLine(color = color, start = Offset(0f, 0f), end = Offset(size.width, size.height), strokeWidth = 2f, pathEffect = dashEffect)
    drawLine(color = color, start = Offset(0f, size.height), end = Offset(size.width, 0f), strokeWidth = 2f, pathEffect = dashEffect)
}

private fun DrawScope.drawContourCircles(color: Color) {
    drawCircle(color = color, radius = size.minDimension * 0.1f, center = Offset(size.width * 0.2f, size.height * 0.3f))
    drawCircle(color = color, radius = size.minDimension * 0.15f, center = Offset(size.width * 0.8f, size.height * 0.7f))
}

@Composable
private fun MapLegendCard() {
    GlassPanel(modifier = Modifier.fillMaxWidth(), contentPadding = 12.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Map Legend",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                LegendItem(color = Color(0xFFFACC15), label = "Me (Live)")
                LegendItem(color = Color(0xFF3B82F6), label = "My History")
                LegendItem(color = Color(0xFF10B981), label = "Contacts")
                LegendItem(color = Color(0xFFA855F7), label = "Mesh Nodes")
                LegendItem(color = Color(0xFFEF4444), label = "Active SOS")
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
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
                        .background(Color(0xFFA855F7))
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
