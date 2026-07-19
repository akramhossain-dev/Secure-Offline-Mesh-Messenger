/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.map

import com.mesh.emergency.core.common.result.Result
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface managing offline map tile storage and layer configuration.
 *
 * All operations are strictly offline — no network tile fetching is performed.
 */
interface MapRepository {

    /** Stream of all stored map layers. */
    fun getMapLayers(): Flow<Result<List<MapLayerModel>>>

    /** Returns the current offline map data snapshot. */
    suspend fun getOfflineMapData(): Result<OfflineMapData>

    /** Persists a map tile for a given layer and coordinates. */
    suspend fun saveMapTile(tile: MapTileModel): Result<Unit>

    /** Retrieves a map tile from cache or online source. */
    suspend fun getMapTile(layerId: String, zoom: Int, x: Int, y: Int): Result<MapTileModel>

    /** Removes all cached tiles for a specific layer. */
    suspend fun clearLayer(layerId: String): Result<Unit>

    /** Returns metadata about available local map coverage. */
    suspend fun getMapBounds(): Result<MapBounds>
}

// ─────────────────────────────────────────────────────────────────────────────
// Domain models
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Represents a named map rendering layer (e.g., satellite, roads, elevation).
 */
data class MapLayerModel(
    val id: String,
    val name: String,
    val isVisible: Boolean = true,
    val opacity: Float = 1.0f,
    val type: MapLayerType = MapLayerType.BASE
)

/**
 * Categories of map rendering layers.
 */
enum class MapLayerType {
    BASE,
    SATELLITE,
    ROADS,
    ELEVATION,
    NODES,
    EMERGENCY_ZONES
}

/**
 * A single offline map tile identified by zoom level and grid coordinates.
 */
data class MapTileModel(
    val layerId: String,
    val zoom: Int,
    val x: Int,
    val y: Int,
    val data: ByteArray,
    val cachedAt: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MapTileModel) return false
        return layerId == other.layerId && zoom == other.zoom && x == other.x && y == other.y
    }

    override fun hashCode(): Int {
        var result = layerId.hashCode()
        result = 31 * result + zoom
        result = 31 * result + x
        result = 31 * result + y
        return result
    }
}

/**
 * Aggregated offline map data snapshot.
 */
data class OfflineMapData(
    val layers: List<MapLayerModel>,
    val bounds: MapBounds,
    val tileCount: Int,
    val lastUpdated: Long
)

/**
 * Geographic bounding box for map coverage.
 */
data class MapBounds(
    val northLatitude: Double = 90.0,
    val southLatitude: Double = -90.0,
    val westLongitude: Double = -180.0,
    val eastLongitude: Double = 180.0
)
