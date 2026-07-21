/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.feature.map

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mesh.emergency.core.common.result.Result
import com.mesh.emergency.core.identity.DeviceFingerprintProvider
import com.mesh.emergency.core.map.MapLayerModel
import com.mesh.emergency.core.map.MapRepository
import com.mesh.emergency.core.map.MapState
import com.mesh.emergency.core.utils.LocationData
import com.mesh.emergency.core.utils.LocationProvider
import com.mesh.emergency.data.local.entity.DbEmergencyStatus
import com.mesh.emergency.data.local.entity.DbEmergencyType
import com.mesh.emergency.data.local.entity.DbMessagePriority
import com.mesh.emergency.data.map.MapProviderImpl
import com.mesh.emergency.domain.repository.LocationRepository
import com.mesh.emergency.domain.repository.NodeDomainModel
import com.mesh.emergency.domain.repository.NodeRepository
import com.mesh.emergency.domain.repository.ResourceDomainModel
import com.mesh.emergency.domain.repository.ResourceRepository
import com.mesh.emergency.feature.emergency.domain.EmergencyEvent
import com.mesh.emergency.feature.emergency.domain.EmergencyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * Enhanced ViewModel for the offline-first MapLibre Map Screen.
 * Integrates:
 * - GPS live coordinates tracking
 * - Predefined & saved offline location geocoding search
 * - Room database mesh peer locations, Emergency SOS, and supply resource layers
 * - Background MBTiles/PMTiles import flow and local style.json generator
 */
@HiltViewModel
class MapViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mapRepository: MapRepository,
    private val mapProvider: MapProviderImpl,
    private val nodeRepository: NodeRepository,
    private val locationProvider: LocationProvider,
    private val locationRepository: LocationRepository,
    private val emergencyRepository: EmergencyRepository,
    private val deviceFingerprintProvider: DeviceFingerprintProvider,
    private val resourceRepository: ResourceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    val mapState: StateFlow<MapState> = mapProvider.mapState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MapState.EMPTY)

    init {
        initStyleJson()
        observeLayers()
        observeNodes()
        observeLiveLocation()
        observeDatabaseLocations()
        observeEmergencyAlerts()
        observeResources()
        mapProvider.markMapReady()
        
        viewModelScope.launch {
            val offlineData = mapRepository.getOfflineMapData()
            if (offlineData is Result.Success) {
                _uiState.update { it.copy(offlineCacheSize = offlineData.data.tileCount) }
            }
        }
    }

    fun initStyleJson() {
        try {
            val styleFile = File(context.filesDir, "style.json")
            val pmtilesFile = File(context.filesDir, "imported_map.pmtiles")
            val hasPmtiles = pmtilesFile.exists()
            _uiState.update { it.copy(isPmtilesLoaded = hasPmtiles) }
            
            val styleContent = if (hasPmtiles) {
                """
                {
                  "version": 8,
                  "name": "OfflineVectorStyle",
                  "sources": {
                    "openmaptiles": {
                      "type": "vector",
                      "url": "pmtiles://${pmtilesFile.absolutePath}"
                    }
                  },
                  "layers": [
                    {
                      "id": "background",
                      "type": "background",
                      "paint": {
                        "background-color": "#0b132b"
                      }
                    },
                    {
                      "id": "water",
                      "type": "fill",
                      "source": "openmaptiles",
                      "source-layer": "water",
                      "paint": {
                        "fill-color": "#38bdf8"
                      }
                    },
                    {
                      "id": "roads",
                      "type": "line",
                      "source": "openmaptiles",
                      "source-layer": "transportation",
                      "paint": {
                        "line-color": "#64748b",
                        "line-width": 1.5
                      }
                    }
                  ]
                }
                """.trimIndent()
            } else {
                val rawStyle = context.assets.open("style.json").bufferedReader().use { it.readText() }
                val geoJsonData = try {
                    context.assets.open("world_full.geojson").bufferedReader().use { it.readText() }
                } catch (e: Exception) {
                    "{\"type\":\"FeatureCollection\",\"features\":[]}"
                }
                rawStyle.replace("\"asset://world_full.geojson\"", geoJsonData)
            }
            styleFile.writeText(styleContent)
            timber.log.Timber.d("Generated style.json successfully at ${styleFile.absolutePath}")
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Failed to write local style.json")
        }
    }

    fun switchMapStyle(style: String) {
        _uiState.update { it.copy(selectedMapStyle = style) }
        try {
            val styleFile = File(context.filesDir, "style.json")
            val geoJsonData = try {
                context.assets.open("world_full.geojson").bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                "{\"type\":\"FeatureCollection\",\"features\":[]}"
            }
            
            val styleContent = if (style == "STREET") {
                """
                {
                  "version": 8,
                  "name": "StreetHDMap",
                  "sources": {
                    "world_data": {
                      "type": "geojson",
                      "data": $geoJsonData
                    },
                    "street-base": {
                      "type": "raster",
                      "tiles": [
                        "https://basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}@2x.png",
                        "https://a.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}@2x.png",
                        "https://b.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}@2x.png"
                      ],
                      "tileSize": 512,
                      "maxzoom": 22
                    }
                  },
                  "layers": [
                    {
                      "id": "background",
                      "type": "background",
                      "paint": { "background-color": "#0F172A" }
                    },
                    {
                      "id": "world-continents",
                      "type": "fill",
                      "source": "world_data",
                      "filter": ["==", "type", "continent"],
                      "paint": { "fill-color": "#1E293B", "fill-opacity": 1.0 }
                    },
                    {
                      "id": "world-countries",
                      "type": "fill",
                      "source": "world_data",
                      "filter": ["==", "type", "country"],
                      "paint": { "fill-color": "#334155", "fill-opacity": 0.95 }
                    },
                    {
                      "id": "world-districts",
                      "type": "fill",
                      "source": "world_data",
                      "filter": ["==", "type", "district"],
                      "paint": { "fill-color": "#475569", "fill-opacity": 0.9 }
                    },
                    {
                      "id": "world-rivers",
                      "type": "line",
                      "source": "world_data",
                      "filter": ["==", "type", "river"],
                      "paint": { "line-color": "#38BDF8", "line-width": 3.5 }
                    },
                    {
                      "id": "world-highways",
                      "type": "line",
                      "source": "world_data",
                      "filter": ["==", "type", "highway"],
                      "paint": { "line-color": "#F59E0B", "line-width": 2.5 }
                    },
                    {
                      "id": "street-tiles",
                      "type": "raster",
                      "source": "street-base",
                      "paint": { "raster-opacity": 1.0 }
                    }
                  ]
                }
                """.trimIndent()
            } else {
                """
                {
                  "version": 8,
                  "name": "SatelliteHybridMap",
                  "sources": {
                    "world_data": {
                      "type": "geojson",
                      "data": $geoJsonData
                    },
                    "satellite": {
                      "type": "raster",
                      "tiles": [
                        "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}"
                      ],
                      "tileSize": 256,
                      "maxzoom": 22
                    },
                    "labels": {
                      "type": "raster",
                      "tiles": [
                        "https://basemaps.cartocdn.com/rastertiles/voyager_only_labels/{z}/{x}/{y}{r}.png"
                      ],
                      "tileSize": 256,
                      "maxzoom": 22
                    }
                  },
                  "layers": [
                    {
                      "id": "background",
                      "type": "background",
                      "paint": { "background-color": "#0B0F19" }
                    },
                    {
                      "id": "world-continents",
                      "type": "fill",
                      "source": "world_data",
                      "filter": ["==", "type", "continent"],
                      "paint": { "fill-color": "#1E293B", "fill-opacity": 1.0 }
                    },
                    {
                      "id": "world-countries",
                      "type": "fill",
                      "source": "world_data",
                      "filter": ["==", "type", "country"],
                      "paint": { "fill-color": "#334155", "fill-opacity": 0.95 }
                    },
                    {
                      "id": "world-districts",
                      "type": "fill",
                      "source": "world_data",
                      "filter": ["==", "type", "district"],
                      "paint": { "fill-color": "#475569", "fill-opacity": 0.9 }
                    },
                    {
                      "id": "world-rivers",
                      "type": "line",
                      "source": "world_data",
                      "filter": ["==", "type", "river"],
                      "paint": { "line-color": "#38BDF8", "line-width": 3.5 }
                    },
                    {
                      "id": "world-highways",
                      "type": "line",
                      "source": "world_data",
                      "filter": ["==", "type", "highway"],
                      "paint": { "line-color": "#F59E0B", "line-width": 2.5 }
                    },
                    {
                      "id": "satellite-tiles",
                      "type": "raster",
                      "source": "satellite",
                      "paint": { "raster-opacity": 1.0 }
                    },
                    {
                      "id": "labels-overlay",
                      "type": "raster",
                      "source": "labels",
                      "paint": { "raster-opacity": 0.95 }
                    }
                  ]
                }
                """.trimIndent()
            }
            styleFile.writeText(styleContent)
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Failed to switch map style")
        }
    }

    private fun observeLayers() {
        viewModelScope.launch {
            mapRepository.getMapLayers().collect { result ->
                when (result) {
                    is Result.Success -> _uiState.update { it.copy(layers = result.data) }
                    is Result.Error   -> _uiState.update { it.copy(errorMessage = result.exception.message) }
                    is Result.Loading -> _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    private fun observeNodes() {
        viewModelScope.launch {
            nodeRepository.getNetworkNodes().collect { result ->
                if (result is Result.Success) {
                    _uiState.update {
                        it.copy(
                            visibleNodes = result.data.filter { n ->
                                n.latitude != 0.0 && n.longitude != 0.0
                            }
                        )
                    }
                }
            }
        }
    }

    private fun observeLiveLocation() {
        viewModelScope.launch {
            locationProvider.getLastKnownLocation().collect { result ->
                if (result is Result.Success) {
                    val locationData = result.data
                    _uiState.update { state ->
                        state.copy(
                            currentLocation = locationData,
                            mapCenterLat = if (state.isAutoCentering) locationData.latitude else state.mapCenterLat,
                            mapCenterLon = if (state.isAutoCentering) locationData.longitude else state.mapCenterLon
                        )
                    }
                }
            }
        }
        viewModelScope.launch {
            locationProvider.getCurrentLocation().collect { result ->
                if (result is Result.Success) {
                    val locationData = result.data
                    _uiState.update { state ->
                        state.copy(
                            currentLocation = locationData,
                            mapCenterLat = if (state.isAutoCentering) locationData.latitude else state.mapCenterLat,
                            mapCenterLon = if (state.isAutoCentering) locationData.longitude else state.mapCenterLon
                        )
                    }
                    locationRepository.saveLocation(locationData)
                }
            }
        }
    }

    private fun observeDatabaseLocations() {
        viewModelScope.launch {
            val myFingerprint = deviceFingerprintProvider.getDeviceFingerprint()
            locationRepository.getAllLocations().collect { result ->
                if (result is Result.Success) {
                    val saved = result.data.filter { it.deviceId == myFingerprint || it.provider == "me" }
                    val shared = result.data.filter { it.deviceId != myFingerprint && it.provider != "me" }
                    _uiState.update { state ->
                        val newestSaved = saved.maxByOrNull { it.timestamp }
                        val centerLat = if (state.currentLocation == null && newestSaved != null) newestSaved.latitude else state.mapCenterLat
                        val centerLon = if (state.currentLocation == null && newestSaved != null) newestSaved.longitude else state.mapCenterLon
                        state.copy(
                            savedLocations = saved,
                            sharedLocations = shared,
                            mapCenterLat = centerLat,
                            mapCenterLon = centerLon
                        )
                    }
                }
            }
        }
    }

    private fun observeEmergencyAlerts() {
        viewModelScope.launch {
            emergencyRepository.getEmergencyEvents().collect { events ->
                _uiState.update { it.copy(emergencyEvents = events) }
            }
        }
    }

    private fun observeResources() {
        viewModelScope.launch {
            resourceRepository.getResources().collect { result ->
                if (result is Result.Success) {
                    _uiState.update { it.copy(resources = result.data) }
                }
            }
        }
    }

    fun onMapMoved(lat: Double, lon: Double) {
        _uiState.update { state ->
            state.copy(
                mapCenterLat = lat.coerceIn(-85.0, 85.0),
                mapCenterLon = lon.coerceIn(-180.0, 180.0),
                isAutoCentering = false
            )
        }
    }

    @android.annotation.SuppressLint("MissingPermission")
    fun onMyLocationClicked() {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
        var lastLoc: LocationData? = _uiState.value.currentLocation
        if (lastLoc == null && lm != null) {
            val providers = listOf(
                android.location.LocationManager.GPS_PROVIDER,
                android.location.LocationManager.NETWORK_PROVIDER,
                android.location.LocationManager.PASSIVE_PROVIDER
            )
            for (p in providers) {
                try {
                    if (lm.isProviderEnabled(p)) {
                        val loc = lm.getLastKnownLocation(p)
                        if (loc != null) {
                            lastLoc = LocationData(
                                id = java.util.UUID.randomUUID().toString(),
                                latitude = loc.latitude,
                                longitude = loc.longitude,
                                altitude = loc.altitude,
                                accuracy = loc.accuracy,
                                timestamp = loc.time,
                                provider = loc.provider ?: "gps",
                                deviceId = deviceFingerprintProvider.getDeviceFingerprint()
                            )
                            break
                        }
                    }
                } catch (_: Exception) {}
            }
        }

        if (lastLoc != null) {
            val targetLat = lastLoc.latitude
            val targetLon = lastLoc.longitude
            _uiState.update { state ->
                state.copy(
                    currentLocation = lastLoc,
                    mapCenterLat = targetLat,
                    mapCenterLon = targetLon,
                    zoomLevel = 16,
                    isAutoCentering = true
                )
            }
        } else {
            _uiState.update { it.copy(isAutoCentering = true) }
        }
    }

    fun downloadMapArea() {
        val centerLat = _uiState.value.mapCenterLat
        val centerLon = _uiState.value.mapCenterLon
        val zoom = _uiState.value.zoomLevel
        
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _uiState.update { it.copy(downloadStatus = "Preparing download...") }
            
            val zooms = listOf(
                (zoom - 1).coerceAtLeast(10),
                zoom,
                (zoom + 1).coerceAtMost(17),
                (zoom + 2).coerceAtMost(17)
            ).distinct()
            
            val tilesToDownload = mutableListOf<Triple<Int, Int, Int>>()
            for (z in zooms) {
                val cx = Math.floor((centerLon + 180.0) / 360.0 * (1 shl z)).toInt()
                val latRad = Math.toRadians(centerLat)
                val cy = Math.floor((1.0 - Math.log(Math.tan(latRad) + 1.0 / Math.cos(latRad)) / Math.PI) / 2.0 * (1 shl z)).toInt()
                
                for (dx in -2..2) {
                    for (dy in -2..2) {
                        tilesToDownload.add(Triple(z, cx + dx, cy + dy))
                    }
                }
            }
            
            val total = tilesToDownload.size
            var downloaded = 0
            
            for (tile in tilesToDownload) {
                val z = tile.first
                val x = tile.second
                val y = tile.third
                
                _uiState.update { 
                    it.copy(
                        downloadStatus = "Downloading tile ${downloaded + 1} of $total",
                        downloadProgress = (downloaded.toFloat() / total)
                    )
                }
                
                mapRepository.getMapTile("base", z, x, y)
                downloaded++
            }
            
            val offlineData = mapRepository.getOfflineMapData()
            val cacheSize = if (offlineData is Result.Success) offlineData.data.tileCount else 0
            
            _uiState.update { 
                it.copy(
                    downloadStatus = "Download complete! Cached $total tiles.",
                    downloadProgress = null,
                    offlineCacheSize = cacheSize
                )
            }
            
            kotlinx.coroutines.delay(3000)
            _uiState.update { it.copy(downloadStatus = null) }
        }
    }

    fun onLayerToggled(layerId: String, isVisible: Boolean) {
        if (layerId == "base") {
            val newStyle = if (_uiState.value.selectedMapStyle == "SATELLITE") "STREET" else "SATELLITE"
            switchMapStyle(newStyle)
            return
        }
        mapProvider.setLayerVisible(layerId, isVisible)
        _uiState.update { state ->
            state.copy(
                layers = state.layers.map { layer ->
                    if (layer.id == layerId) layer.copy(isVisible = isVisible) else layer
                }
            )
        }
    }

    fun onZoomIn() {
        _uiState.update { it.copy(zoomLevel = (it.zoomLevel + 1).coerceAtMost(20), isAutoCentering = true) }
    }

    fun onZoomOut() {
        _uiState.update { it.copy(zoomLevel = (it.zoomLevel - 1).coerceAtLeast(3), isAutoCentering = true) }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }
        
        viewModelScope.launch {
            val results = mutableListOf<SearchResult>()
            // 1. Predefined places
            results.addAll(defaultPlaces.filter { it.name.contains(query, ignoreCase = true) || it.type.contains(query, ignoreCase = true) })
            
            // 2. Saved locations
            val savedResults = _uiState.value.savedLocations.filter {
                (it.provider ?: "").contains(query, ignoreCase = true) || "Saved Place".contains(query, ignoreCase = true)
            }.map {
                SearchResult(
                    name = it.provider ?: "Saved Place",
                    type = "Saved Place",
                    latitude = it.latitude,
                    longitude = it.longitude
                )
            }
            results.addAll(savedResults)
            
            _uiState.update { it.copy(searchResults = results, isSearching = true) }
        }
    }

    fun selectSearchResult(result: SearchResult) {
        _uiState.update { state ->
            state.copy(
                mapCenterLat = result.latitude,
                mapCenterLon = result.longitude,
                zoomLevel = 16,
                isAutoCentering = true,
                searchQuery = "",
                isSearching = false,
                searchResults = emptyList()
            )
        }
    }

    fun shareCurrentLocation() {
        val loc = _uiState.value.currentLocation ?: return
        viewModelScope.launch {
            locationRepository.saveLocation(
                loc.copy(
                    id = "loc_share_${System.currentTimeMillis()}",
                    provider = "me"
                )
            )
            _uiState.update { it.copy(downloadStatus = "Location shared with mesh network!") }
            kotlinx.coroutines.delay(2000)
            _uiState.update { it.copy(downloadStatus = null) }
        }
    }

    fun createEmergencyMarker(
        name: String,
        type: String, // "SOS", "Rescue Point", "FOOD", "WATER", "SHELTER", "MEDICAL"
        description: String,
        lat: Double,
        lon: Double
    ) {
        viewModelScope.launch {
            if (type == "SOS" || type == "Rescue Point") {
                val dbType = when (type) {
                    "SOS" -> DbEmergencyType.SOS
                    else -> DbEmergencyType.DISASTER
                }
                val event = EmergencyEvent(
                    id = "sos_${System.currentTimeMillis()}",
                    type = dbType,
                    priority = DbMessagePriority.CRITICAL,
                    status = DbEmergencyStatus.CREATED,
                    senderId = deviceFingerprintProvider.getDeviceFingerprint(),
                    message = "$name: $description",
                    latitude = lat,
                    longitude = lon,
                    timestamp = System.currentTimeMillis(),
                    isResolved = false,
                    ttl = System.currentTimeMillis() + 86400000L
                )
                emergencyRepository.createEmergencyEvent(event)
            } else {
                val resource = ResourceDomainModel(
                    id = "res_${System.currentTimeMillis()}",
                    ownerId = deviceFingerprintProvider.getDeviceFingerprint(),
                    name = name,
                    type = type.uppercase(),
                    quantity = 1,
                    latitude = lat,
                    longitude = lon,
                    description = description,
                    status = "AVAILABLE",
                    privacy = "PUBLIC",
                    ttl = System.currentTimeMillis() + 86400000L
                )
                resourceRepository.saveResource(resource)
            }
            
            _uiState.update { it.copy(downloadStatus = "Marker '$name' added successfully!") }
            kotlinx.coroutines.delay(2000)
            _uiState.update { it.copy(downloadStatus = null) }
        }
    }

    fun importOfflineMap(uri: Uri) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _uiState.update { it.copy(downloadStatus = "Analyzing imported map file...") }
            try {
                val contentResolver = context.contentResolver
                val fileName = getFileName(context, uri) ?: "imported_map.mbtiles"
                
                if (fileName.endsWith(".pmtiles", ignoreCase = true)) {
                    val destFile = File(context.filesDir, "imported_map.pmtiles")
                    contentResolver.openInputStream(uri)?.use { input ->
                        destFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    
                    initStyleJson()
                    
                    _uiState.update {
                        it.copy(
                            downloadStatus = "Successfully imported vector PMTiles map!",
                            downloadProgress = null
                        )
                    }
                } else {
                    // Extract tiles from MBTiles
                    val tempFile = File(context.cacheDir, "temp_import.mbtiles")
                    contentResolver.openInputStream(uri)?.use { input ->
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    
                    _uiState.update { it.copy(downloadStatus = "Extracting tiles from MBTiles...") }
                    
                    val db = android.database.sqlite.SQLiteDatabase.openDatabase(
                        tempFile.absolutePath,
                        null,
                        android.database.sqlite.SQLiteDatabase.OPEN_READONLY
                    )
                    
                    val cursor = db.rawQuery("SELECT zoom_level, tile_column, tile_row, tile_data FROM tiles", null)
                    val total = cursor.count
                    var imported = 0
                    
                    val colZoom = cursor.getColumnIndex("zoom_level")
                    val colCol = cursor.getColumnIndex("tile_column")
                    val colRow = cursor.getColumnIndex("tile_row")
                    val colData = cursor.getColumnIndex("tile_data")
                    
                    db.beginTransaction()
                    try {
                        while (cursor.moveToNext()) {
                            val z = cursor.getInt(colZoom)
                            val x = cursor.getInt(colCol)
                            val yRaw = cursor.getInt(colRow)
                            val data = cursor.getBlob(colData)
                            
                            val y = (1 shl z) - 1 - yRaw
                            
                            val file = File(context.cacheDir, "map_tiles/base/$z/$x/$y.png")
                            file.parentFile?.mkdirs()
                            file.writeBytes(data)
                            
                            imported++
                            if (imported % 100 == 0 || imported == total) {
                                _uiState.update {
                                    it.copy(
                                        downloadStatus = "Imported $imported of $total tiles",
                                        downloadProgress = imported.toFloat() / total
                                    )
                                }
                            }
                        }
                        db.setTransactionSuccessful()
                    } finally {
                        db.endTransaction()
                        cursor.close()
                        db.close()
                        tempFile.delete()
                    }
                    
                    val offlineData = mapRepository.getOfflineMapData()
                    val cacheSize = if (offlineData is Result.Success) offlineData.data.tileCount else 0
                    
                    _uiState.update {
                        it.copy(
                            downloadStatus = "Import complete! Stored $total tiles.",
                            downloadProgress = null,
                            offlineCacheSize = cacheSize
                        )
                    }
                }
            } catch (e: Exception) {
                timber.log.Timber.e(e, "Failed to import offline map")
                _uiState.update {
                    it.copy(
                        downloadStatus = "Import failed: ${e.message}",
                        downloadProgress = null
                    )
                }
            }
            
            kotlinx.coroutines.delay(3000)
            _uiState.update { it.copy(downloadStatus = null) }
        }
    }

    fun deleteOfflineMap() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _uiState.update { it.copy(downloadStatus = "Deleting offline map data...") }
            try {
                val rasterDir = File(context.cacheDir, "map_tiles")
                if (rasterDir.exists()) {
                    rasterDir.deleteRecursively()
                }
                
                val pmtilesFile = File(context.filesDir, "imported_map.pmtiles")
                if (pmtilesFile.exists()) {
                    pmtilesFile.delete()
                }
                
                initStyleJson()
                
                val offlineData = mapRepository.getOfflineMapData()
                val cacheSize = if (offlineData is Result.Success) offlineData.data.tileCount else 0
                
                _uiState.update {
                    it.copy(
                        downloadStatus = "Offline map data deleted successfully.",
                        offlineCacheSize = cacheSize,
                        downloadProgress = null
                    )
                }
            } catch (e: Exception) {
                timber.log.Timber.e(e, "Failed to delete offline map data")
                _uiState.update { it.copy(downloadStatus = "Failed to delete map: ${e.message}") }
            }
            
            kotlinx.coroutines.delay(3000)
            _uiState.update { it.copy(downloadStatus = null) }
        }
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val colIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (colIdx >= 0) {
                        result = cursor.getString(colIdx)
                    }
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

data class SearchResult(
    val name: String,
    val type: String, // "City", "Area", "Road", "Landmark", "Saved Place"
    val latitude: Double,
    val longitude: Double
)

private val defaultPlaces = listOf(
    SearchResult("Dhaka City", "Capital City", 23.8103, 90.4125),
    SearchResult("Jatrabari", "City Hub", 23.7118, 90.4350),
    SearchResult("Motijheel", "Financial District", 23.7330, 90.4172),
    SearchResult("Dhanmondi", "Residential Area", 23.7461, 90.3742),
    SearchResult("Gulshan", "Diplomatic Zone", 23.7925, 90.4078),
    SearchResult("Banani", "Commercial Area", 23.7937, 90.4047),
    SearchResult("Uttara", "Suburban Sector", 23.8759, 90.3795),
    SearchResult("Mirpur", "Metro Hub", 23.8069, 90.3687),
    SearchResult("Demra", "Industrial Area", 23.7081, 90.4950),
    SearchResult("Matuail", "South Dhaka District", 23.7012, 90.4632),
    SearchResult("Shyamoli", "Commercial District", 23.7772, 90.3755),
    SearchResult("Sadarghat / Old Dhaka", "Port & Heritage Zone", 23.7099, 90.4071),
    SearchResult("Chittagong (Chattogram)", "Port City", 22.3569, 91.7832),
    SearchResult("Sylhet", "Divisional City", 24.8949, 91.8687),
    SearchResult("Rajshahi", "Educational City", 24.3745, 88.6042),
    SearchResult("Khulna", "Divisional City", 22.8456, 89.5403),
    SearchResult("Barishal", "Divisional City", 22.7010, 90.3535),
    SearchResult("Rangpur", "Divisional City", 25.7439, 89.2752),
    SearchResult("Mymensingh", "Divisional City", 24.7471, 90.4203),
    SearchResult("Cumilla", "City District", 23.4607, 91.1809),
    SearchResult("Cox's Bazar", "Coastal Tourism Hub", 21.4272, 92.0058),
    SearchResult("Mugda Medical College & Hospital", "Emergency Medical Center", 23.7289, 90.4285),
    SearchResult("Dhaka Medical College Hospital", "Primary Trauma Center", 23.7260, 90.3976),
    SearchResult("Square Hospital", "Medical Center", 23.7531, 90.3816),
    SearchResult("United Hospital Gulshan", "Medical Center", 23.7978, 90.4162),
    SearchResult("Matuail Emergency Shelter", "Disaster Rescue Center", 23.7025, 90.4650),
    SearchResult("Central Police HQ", "Emergency Services", 23.7388, 90.4150),
    SearchResult("Fire Service & Civil Defence HQ", "Emergency Response", 23.7214, 90.4061)
)

data class MapUiState(
    val isLoading: Boolean = false,
    val layers: List<MapLayerModel> = emptyList(),
    val visibleNodes: List<NodeDomainModel> = emptyList(),
    val savedLocations: List<LocationData> = emptyList(),
    val sharedLocations: List<LocationData> = emptyList(),
    val emergencyEvents: List<EmergencyEvent> = emptyList(),
    val resources: List<ResourceDomainModel> = emptyList(),
    val currentLocation: LocationData? = null,
    val zoomLevel: Int = 14,
    val errorMessage: String? = null,
    val mapCenterLat: Double = 23.8103,
    val mapCenterLon: Double = 90.4125,
    val isAutoCentering: Boolean = true,
    val downloadProgress: Float? = null,
    val downloadStatus: String? = null,
    val offlineCacheSize: Int = 0,
    val searchQuery: String = "",
    val searchResults: List<SearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val isPmtilesLoaded: Boolean = false,
    val selectedMapStyle: String = "SATELLITE"
)
