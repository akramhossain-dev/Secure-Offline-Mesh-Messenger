/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.map

import com.mesh.emergency.core.common.result.Result
import com.mesh.emergency.core.map.MapBounds
import com.mesh.emergency.core.map.MapLayerModel
import com.mesh.emergency.core.map.MapLayerType
import com.mesh.emergency.core.map.MapRepository
import com.mesh.emergency.core.map.MapTileModel
import com.mesh.emergency.core.map.OfflineMapData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.util.LinkedHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline-first implementation of [MapRepository].
 *
 * Uses an in-memory LRU tile cache for performance. No network calls are made.
 * Map layers are pre-configured for emergency use cases.
 */
@Singleton
class MapRepositoryImpl @Inject constructor() : MapRepository {

    // ── LRU tile cache (max 100 tiles ≈ ~10 MB at 100 KB per tile) ────────────
    private val tileCache: LinkedHashMap<String, MapTileModel> = object :
        LinkedHashMap<String, MapTileModel>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<String, MapTileModel>): Boolean {
            return size > MAX_CACHE_SIZE
        }
    }

    private val _layers = MutableStateFlow(defaultLayers())

    override fun getMapLayers(): Flow<Result<List<MapLayerModel>>> =
        _layers.map { Result.Success(it) }

    override suspend fun getOfflineMapData(): Result<OfflineMapData> {
        return try {
            val layers = _layers.value
            Result.Success(
                OfflineMapData(
                    layers = layers,
                    bounds = MapBounds(),
                    tileCount = tileCache.size,
                    lastUpdated = System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun saveMapTile(tile: MapTileModel): Result<Unit> {
        return try {
            val key = tileKey(tile.layerId, tile.zoom, tile.x, tile.y)
            synchronized(tileCache) {
                tileCache[key] = tile
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun clearLayer(layerId: String): Result<Unit> {
        return try {
            synchronized(tileCache) {
                tileCache.entries.removeIf { it.key.startsWith("$layerId:") }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getMapBounds(): Result<MapBounds> =
        Result.Success(MapBounds())

    fun updateLayerVisibility(layerId: String, isVisible: Boolean) {
        _layers.value = _layers.value.map { layer ->
            if (layer.id == layerId) layer.copy(isVisible = isVisible) else layer
        }
    }

    private fun tileKey(layerId: String, zoom: Int, x: Int, y: Int) = "$layerId:$zoom:$x:$y"

    private fun defaultLayers(): List<MapLayerModel> = listOf(
        MapLayerModel(id = "base",      name = "Base Map",       type = MapLayerType.BASE,           isVisible = true),
        MapLayerModel(id = "roads",     name = "Roads",          type = MapLayerType.ROADS,          isVisible = true),
        MapLayerModel(id = "nodes",     name = "Mesh Nodes",     type = MapLayerType.NODES,          isVisible = true),
        MapLayerModel(id = "emergency", name = "Emergency Zones",type = MapLayerType.EMERGENCY_ZONES,isVisible = true),
        MapLayerModel(id = "elevation", name = "Elevation",      type = MapLayerType.ELEVATION,      isVisible = false)
    )

    companion object {
        private const val MAX_CACHE_SIZE = 100
    }
}
