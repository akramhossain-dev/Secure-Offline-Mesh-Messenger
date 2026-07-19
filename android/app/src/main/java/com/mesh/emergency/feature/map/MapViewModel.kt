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
            val cacheDir = context.cacheDir.absolutePath
            val pmtilesFile = File(context.filesDir, "imported_map.pmtiles")
            
            val styleContent = if (pmtilesFile.exists()) {
                // Vector style using local PMTiles source!
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
                        "background-color": "#1e293b"
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
                    },
                    {
                      "id": "buildings",
                      "type": "fill",
                      "source": "openmaptiles",
                      "source-layer": "building",
                      "paint": {
                        "fill-color": "#475569"
                      }
                    }
                  ]
                }
                """.trimIndent()
            } else {
                // Raster style using local slippy map png tiles!
                """
                {
                  "version": 8,
                  "name": "OfflineRasterStyle",
                  "sources": {
                    "base": {
                      "type": "raster",
                      "tiles": [
                        "file://${cacheDir}/map_tiles/base/{z}/{x}/{y}.png"
                      ],
                      "tileSize": 256,
                      "minzoom": 10,
                      "maxzoom": 17
                    }
                  },
                  "layers": [
                    {
                      "id": "background",
                      "type": "background",
                      "paint": {
                        "background-color": "#090D1A"
                      }
                    },
                    {
                      "id": "base-tiles",
                      "type": "raster",
                      "source": "base",
                      "paint": {
                        "raster-opacity": 0.9
                      }
                    }
                  ]
                }
                """.trimIndent()
            }
            styleFile.writeText(styleContent)
            timber.log.Timber.d("Generated style.json successfully at ${styleFile.absolutePath}")
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Failed to write local style.json")
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
                    _uiState.update {
                        it.copy(
                            savedLocations = saved,
                            sharedLocations = shared
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

    fun onMyLocationClicked() {
        _uiState.update { state ->
            val cur = state.currentLocation
            if (cur != null) {
                state.copy(
                    mapCenterLat = cur.latitude,
                    mapCenterLon = cur.longitude,
                    zoomLevel = 15,
                    isAutoCentering = true
                )
            } else {
                state.copy(isAutoCentering = true)
            }
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
        _uiState.update { it.copy(zoomLevel = (it.zoomLevel + 1).coerceAtMost(17)) }
    }

    fun onZoomOut() {
        _uiState.update { it.copy(zoomLevel = (it.zoomLevel - 1).coerceAtLeast(10)) }
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
            results.addAll(defaultPlaces.filter { it.name.contains(query, ignoreCase = true) })
            
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
    SearchResult("Dhaka", "City", 23.8103, 90.4125),
    SearchResult("Uttara", "Area", 23.8759, 90.3795),
    SearchResult("Gulshan", "Area", 23.7925, 90.4078),
    SearchResult("Banani", "Area", 23.7937, 90.4067),
    SearchResult("Mirpur", "Area", 23.8069, 90.3687),
    SearchResult("Dhanmondi", "Area", 23.7461, 90.3742),
    SearchResult("Lalbagh Fort", "Landmark", 23.7189, 90.3882),
    SearchResult("National Museum", "Landmark", 23.7375, 90.3927),
    SearchResult("Ahsan Manzil", "Landmark", 23.7086, 90.4060),
    SearchResult("Parliament House", "Landmark", 23.7625, 90.3786),
    SearchResult("Pragati Sarani", "Road", 23.7890, 90.4223),
    SearchResult("Mirpur Road", "Road", 23.7650, 90.3800),
    SearchResult("Kazi Nazrul Islam Avenue", "Road", 23.7510, 90.3940),
    SearchResult("Airport Road", "Road", 23.8400, 90.4000)
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
    val isSearching: Boolean = false
)
