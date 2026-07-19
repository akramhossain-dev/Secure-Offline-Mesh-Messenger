/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.feature.map

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mesh.emergency.R
import com.mesh.emergency.core.common.extensions.hasPermission
import com.mesh.emergency.core.designsystem.component.AuroraBackdrop
import com.mesh.emergency.core.designsystem.component.GlassPanel
import com.mesh.emergency.core.designsystem.component.PermissionRequiredState
import com.mesh.emergency.core.designsystem.theme.MeshThemeTokens
import com.mesh.emergency.core.map.MapLayerModel
import com.mesh.emergency.core.map.MapState
import com.mesh.emergency.core.utils.LocationData
import com.mesh.emergency.domain.repository.NodeDomainModel
import com.mesh.emergency.domain.repository.ResourceDomainModel
import com.mesh.emergency.feature.emergency.domain.EmergencyEvent
import java.io.File

/**
 * Redesigned full-screen Offline Map Screen powered by MapLibre SDK.
 * Features:
 * - Google Maps-inspired search and location autocomplete.
 * - Local MBTiles and PMTiles offline importer.
 * - Emergency markers long-press creation dialog.
 * - MapLibre native lifecycle MapView integrations.
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

    // Document Picker for MBTiles/PMTiles import
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importOfflineMap(uri)
        }
    }

    // Long press marker creation dialog state
    var showCreateMarkerDialog by remember { mutableStateOf(false) }
    var clickedLatitude by remember { mutableStateOf(0.0) }
    var clickedLongitude by remember { mutableStateOf(0.0) }

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
                modifier = Modifier.fillMaxSize()
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // ── 1. MapLibre Native MapView Wrapper ────────────────────
                    MapLibreMapView(
                        mapCenterLat = uiState.mapCenterLat,
                        mapCenterLon = uiState.mapCenterLon,
                        zoomLevel = uiState.zoomLevel,
                        isAutoCentering = uiState.isAutoCentering,
                        visibleNodes = uiState.visibleNodes,
                        emergencyEvents = uiState.emergencyEvents,
                        resources = uiState.resources,
                        savedLocations = uiState.savedLocations,
                        sharedLocations = uiState.sharedLocations,
                        currentLocation = uiState.currentLocation,
                        layers = uiState.layers,
                        onMapMoved = { lat, lon -> viewModel.onMapMoved(lat, lon) },
                        onMapLongClick = { lat, lon ->
                            clickedLatitude = lat
                            clickedLongitude = lon
                            showCreateMarkerDialog = true
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // ── 2. Floating Google Maps inspired Search capsule ───────
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, start = 16.dp, end = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.onSearchQueryChanged(it) },
                            placeholder = { Text("Search offline places...", color = Color.White.copy(alpha = 0.6f)) },
                            leadingIcon = {
                                IconButton(onClick = onBack) {
                                    Icon(
                                        Icons.AutoMirrored.Default.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color.White
                                    )
                                }
                            },
                            trailingIcon = {
                                Row {
                                    if (uiState.searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.White)
                                        }
                                    }
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = "Search",
                                        tint = Color.White,
                                        modifier = Modifier.padding(end = 12.dp, top = 12.dp)
                                    )
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(28.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.Black.copy(alpha = 0.75f),
                                unfocusedContainerColor = Color.Black.copy(alpha = 0.65f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Autocomplete Search Results dropdown list
                        if (uiState.isSearching && uiState.searchResults.isNotEmpty()) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.9f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 240.dp)
                            ) {
                                LazyColumn(
                                    contentPadding = PaddingValues(8.dp)
                                ) {
                                    items(uiState.searchResults) { result ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    viewModel.onMapMoved(result.latitude, result.longitude)
                                                    viewModel.onSearchQueryChanged("")
                                                }
                                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text(result.name, color = Color.White, fontWeight = FontWeight.Bold)
                                                Text(result.type, color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
                                            }
                                            Text(
                                                String.format("%.4f, %.4f", result.latitude, result.longitude),
                                                color = MaterialTheme.colorScheme.primary,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                                    }
                                }
                            }
                        }

                        // Layer Filters Toggle chips
                        LayerToggleRow(
                            layers = uiState.layers,
                            onToggle = { id, vis -> viewModel.onLayerToggled(id, vis) }
                        )

                        // Translucent status banner feedback
                        uiState.downloadStatus?.let { status ->
                            GlassPanel(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = 10.dp
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = status,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White
                                    )
                                    uiState.downloadProgress?.let { progress ->
                                        Spacer(Modifier.height(6.dp))
                                        LinearProgressIndicator(
                                            progress = { progress },
                                            modifier = Modifier.fillMaxWidth(),
                                            color = MaterialTheme.colorScheme.primary,
                                            trackColor = Color.White.copy(alpha = 0.2f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ── 3. Bottom Controls Panel overlay ──────────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                // Bottom Left Action Panel
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Local import database file trigger
                                    Button(
                                        onClick = { importLauncher.launch(arrayOf("*/*")) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.tertiary,
                                            contentColor = Color.White
                                        ),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Import Map", style = MaterialTheme.typography.labelSmall)
                                    }

                                    // Share coordinates trigger
                                    Button(
                                        onClick = { viewModel.shareCurrentLocation() },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondary,
                                            contentColor = Color.White
                                        ),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Share Location", style = MaterialTheme.typography.labelSmall)
                                    }
                                }

                                // Bottom Right: Camera zoom and orientation FAB column
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    // Zoom In FAB
                                    FloatingActionButton(
                                        onClick = { viewModel.onZoomIn() },
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = Color.White,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Zoom In")
                                    }
                                    // Zoom Out FAB
                                    FloatingActionButton(
                                        onClick = { viewModel.onZoomOut() },
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = Color.White,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "Zoom Out")
                                    }
                                    // Re-center / Follow My Location
                                    FloatingActionButton(
                                        onClick = { viewModel.onMyLocationClicked() },
                                        containerColor = if (uiState.isAutoCentering) MeshThemeTokens.semanticColors.connected else Color.Black.copy(alpha = 0.65f),
                                        contentColor = if (uiState.isAutoCentering) Color.Black else Color.White,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(Icons.Default.MyLocation, contentDescription = "My Location")
                                    }
                                }
                            }

                            // Storage display & download/management bottom card
                            GlassPanel(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = 12.dp
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Offline Storage Map System",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Cached tiles: ${uiState.offlineCacheSize} (${String.format("%.2f", uiState.offlineCacheSize * 0.015)} MB)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.7f)
                                        )
                                    }
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        IconButton(
                                            onClick = { viewModel.downloadMapArea() },
                                            colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                                        ) {
                                            Icon(Icons.Default.Download, contentDescription = "Download Region", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                        }
                                        IconButton(
                                            onClick = { viewModel.deleteOfflineMap() },
                                            colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Red.copy(alpha = 0.2f))
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Clear Cache", tint = Color.Red)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ── 4. Long Press marker creation dialog ──────────────────
                    if (showCreateMarkerDialog) {
                        var markerName by remember { mutableStateOf("") }
                        var markerDesc by remember { mutableStateOf("") }
                        var markerType by remember { mutableStateOf("Food") }
                        val categories = listOf("SOS", "Rescue Point", "Food", "Water", "Shelter", "Medical")
                        
                        AlertDialog(
                            onDismissRequest = { showCreateMarkerDialog = false },
                            title = { Text("Add Emergency/Supply Marker") },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Coordinates: ${String.format("%.4f, %.4f", clickedLatitude, clickedLongitude)}", style = MaterialTheme.typography.labelMedium)
                                    
                                    OutlinedTextField(
                                        value = markerName,
                                        onValueChange = { markerName = it },
                                        label = { Text("Name / Title") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    
                                    OutlinedTextField(
                                        value = markerDesc,
                                        onValueChange = { markerDesc = it },
                                        label = { Text("Description") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    
                                    Text("Category:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                    
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        items(categories) { cat ->
                                            FilterChip(
                                                selected = markerType == cat,
                                                onClick = { markerType = cat },
                                                label = { Text(cat, style = MaterialTheme.typography.labelSmall) }
                                            )
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        if (markerName.isNotBlank()) {
                                            viewModel.createEmergencyMarker(
                                                name = markerName,
                                                type = markerType,
                                                description = markerDesc,
                                                lat = clickedLatitude,
                                                lon = clickedLongitude
                                            )
                                            showCreateMarkerDialog = false
                                        }
                                    }
                                ) {
                                    Text("Save Marker")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showCreateMarkerDialog = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
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
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    containerColor = Color.Black.copy(alpha = 0.5f),
                    labelColor = Color.White.copy(alpha = 0.8f)
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = layer.isVisible,
                    borderColor = Color.White.copy(alpha = 0.2f),
                    selectedBorderColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}

/**
 * Native MapLibre view wrapper that is lifecycle aware.
 * Projects nodes, SOS alerts, and supply resources onto the map coordinates using native MapLibre Annotations.
 */
@Composable
private fun MapLibreMapView(
    mapCenterLat: Double,
    mapCenterLon: Double,
    zoomLevel: Int,
    isAutoCentering: Boolean,
    visibleNodes: List<NodeDomainModel>,
    emergencyEvents: List<EmergencyEvent>,
    resources: List<ResourceDomainModel>,
    savedLocations: List<LocationData>,
    sharedLocations: List<LocationData>,
    currentLocation: LocationData?,
    layers: List<MapLayerModel>,
    onMapMoved: (Double, Double) -> Unit,
    onMapLongClick: (Double, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    val mapView = remember {
        org.maplibre.android.maps.MapView(context)
    }

    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, mapView) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_START -> mapView.onStart()
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> mapView.onResume()
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> mapView.onStop()
                androidx.lifecycle.Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }

    var mapboxMapState by remember { mutableStateOf<org.maplibre.android.maps.MapLibreMap?>(null) }

    AndroidView(
        factory = { mapView },
        modifier = modifier
    ) { view ->
        if (mapboxMapState == null) {
            view.getMapAsync { mapboxMap ->
                mapboxMapState = mapboxMap
                
                val styleFile = File(context.filesDir, "style.json")
                if (styleFile.exists()) {
                    mapboxMap.setStyle(org.maplibre.android.maps.Style.Builder().fromUri("file://${styleFile.absolutePath}"))
                }
                
                mapboxMap.addOnCameraMoveListener {
                    val cameraPos = mapboxMap.cameraPosition
                    val target = cameraPos?.target
                    if (target != null) {
                        onMapMoved(target.latitude, target.longitude)
                    }
                }

                mapboxMap.addOnMapLongClickListener { latLng ->
                    onMapLongClick(latLng.latitude, latLng.longitude)
                    true
                }
            }
        }
    }

    // Dynamic Camera Tracking Updates
    LaunchedEffect(mapCenterLat, mapCenterLon, zoomLevel, isAutoCentering) {
        val map = mapboxMapState ?: return@LaunchedEffect
        val currentCamera = map.cameraPosition ?: return@LaunchedEffect
        val target = currentCamera.target ?: return@LaunchedEffect
        val distance = Math.abs(target.latitude - mapCenterLat) + Math.abs(target.longitude - mapCenterLon)
        val currentZoom = currentCamera.zoom.toDouble()
        val isDifferentZoom = Math.abs(currentZoom - zoomLevel.toDouble()) > 0.1
        
        if (distance > 0.0001 || isDifferentZoom) {
            map.animateCamera(
                org.maplibre.android.camera.CameraUpdateFactory.newLatLngZoom(
                    org.maplibre.android.geometry.LatLng(mapCenterLat, mapCenterLon),
                    zoomLevel.toDouble()
                ),
                800
            )
        }
    }

    // Dynamic Markers rendering
    LaunchedEffect(mapboxMapState, visibleNodes, emergencyEvents, resources, savedLocations, sharedLocations, currentLocation, layers) {
        val map = mapboxMapState ?: return@LaunchedEffect
        map.clear()
        
        // 1. Me location
        if (currentLocation != null) {
            map.addMarker(
                org.maplibre.android.annotations.MarkerOptions()
                    .position(org.maplibre.android.geometry.LatLng(currentLocation.latitude, currentLocation.longitude))
                    .title("Me (Current)")
            )
        }

        // 2. Emergency Events
        val showEmergency = layers.any { it.id == "emergency" && it.isVisible }
        if (showEmergency) {
            emergencyEvents.forEach { event ->
                if (!event.isResolved) {
                    map.addMarker(
                        org.maplibre.android.annotations.MarkerOptions()
                            .position(org.maplibre.android.geometry.LatLng(event.latitude, event.longitude))
                            .title(event.type.name)
                            .snippet(event.message)
                    )
                }
            }
        }

        // 3. Resources
        resources.forEach { res ->
            map.addMarker(
                org.maplibre.android.annotations.MarkerOptions()
                    .position(org.maplibre.android.geometry.LatLng(res.latitude, res.longitude))
                    .title(res.type)
                    .snippet("${res.name}: ${res.description}")
            )
        }

        // 4. Mesh Nodes
        val showNodes = layers.any { it.id == "nodes" && it.isVisible }
        if (showNodes) {
            visibleNodes.forEach { node ->
                map.addMarker(
                    org.maplibre.android.annotations.MarkerOptions()
                        .position(org.maplibre.android.geometry.LatLng(node.latitude, node.longitude))
                        .title("Mesh Node: ${node.id}")
                        .snippet("Battery: ${node.batteryLevel}% • RSSI: ${node.rssi} dBM")
                )
            }
        }

        // 5. Saved history locations
        val showRoads = layers.any { it.id == "roads" && it.isVisible }
        if (showRoads) {
            savedLocations.forEach { loc ->
                map.addMarker(
                    org.maplibre.android.annotations.MarkerOptions()
                        .position(org.maplibre.android.geometry.LatLng(loc.latitude, loc.longitude))
                        .title("History Path")
                )
            }
        }
    }
}
