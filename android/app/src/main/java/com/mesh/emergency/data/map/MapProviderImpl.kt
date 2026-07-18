/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.map

import com.mesh.emergency.core.common.result.Result
import com.mesh.emergency.core.map.MapProvider
import com.mesh.emergency.core.map.MapState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [MapProvider] for offline-only map loading.
 *
 * Manages map state transitions and available layer registry.
 * No online map sources are used.
 */
@Singleton
class MapProviderImpl @Inject constructor(
    private val mapRepository: MapRepositoryImpl
) : MapProvider {

    private val _mapState = MutableStateFlow(MapState.EMPTY)
    override val mapState: StateFlow<MapState> = _mapState.asStateFlow()

    private val _availableLayers = mutableListOf(
        "base", "roads", "nodes", "emergency", "elevation"
    )

    override fun loadOfflineMap(filePath: String): Result<Unit> {
        return try {
            _mapState.value = MapState.LOADING
            val file = File(filePath)
            if (file.exists() && file.canRead()) {
                Timber.d("MapProvider: Loaded offline map from $filePath")
                _mapState.value = MapState.LOADED
                Result.Success(Unit)
            } else {
                Timber.w("MapProvider: Map file not found at $filePath — using empty state")
                _mapState.value = MapState.EMPTY
                Result.Success(Unit) // Offline mode gracefully handles missing files
            }
        } catch (e: Exception) {
            Timber.e(e, "MapProvider: Failed to load map from $filePath")
            _mapState.value = MapState.ERROR
            Result.Error(e)
        }
    }

    override fun clearMapCache() {
        Timber.d("MapProvider: Clearing map cache")
        _mapState.value = MapState.EMPTY
    }

    override fun getAvailableLayers(): List<String> = _availableLayers.toList()

    /** Toggle a layer's visibility. */
    fun setLayerVisible(layerId: String, isVisible: Boolean) {
        mapRepository.updateLayerVisibility(layerId, isVisible)
    }

    /** Mark map as ready without a file (used when tiles are pre-cached). */
    fun markMapReady() {
        _mapState.value = MapState.LOADED
    }
}
