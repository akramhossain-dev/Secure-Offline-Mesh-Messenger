/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.map

import android.content.Context
import com.mesh.emergency.core.common.result.Result
import com.mesh.emergency.core.map.MapBounds
import com.mesh.emergency.core.map.MapLayerModel
import com.mesh.emergency.core.map.MapLayerType
import com.mesh.emergency.core.map.MapRepository
import com.mesh.emergency.core.map.MapTileModel
import com.mesh.emergency.core.map.OfflineMapData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.io.File
import java.util.LinkedHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline-first implementation of [MapRepository].
 *
 * Uses an in-memory LRU tile cache and persistent cacheDir directory storage.
 */
@Singleton
class MapRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : MapRepository {

    // ── LRU tile cache (max 100 tiles ≈ ~10 MB at 100 KB per tile) ────────────
    private val tileCache: LinkedHashMap<String, MapTileModel> = object :
        LinkedHashMap<String, MapTileModel>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<String, MapTileModel>): Boolean {
            val shouldEvict = size > MAX_CACHE_SIZE
            if (shouldEvict) {
                val tile = eldest.value
                try {
                    val file = getTileFile(tile.layerId, tile.zoom, tile.x, tile.y)
                    if (file.exists()) {
                        file.delete()
                    }
                } catch (e: Exception) {
                    // Fail silently during initial initialization or tests
                }
            }
            return shouldEvict
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
                    tileCount = countCachedTiles(),
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
            val file = getTileFile(tile.layerId, tile.zoom, tile.x, tile.y)
            file.parentFile?.mkdirs()
            file.writeBytes(tile.data)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getMapTile(layerId: String, zoom: Int, x: Int, y: Int): Result<MapTileModel> {
        val key = tileKey(layerId, zoom, x, y)
        synchronized(tileCache) {
            tileCache[key]?.let { return Result.Success(it) }
        }

        // Try reading from file system cache
        val file = getTileFile(layerId, zoom, x, y)
        if (file.exists() && file.canRead()) {
            try {
                val bytes = file.readBytes()
                val tile = MapTileModel(layerId, zoom, x, y, bytes)
                synchronized(tileCache) {
                    tileCache[key] = tile
                }
                return Result.Success(tile)
            } catch (e: Exception) {
                timber.log.Timber.e(e, "Failed to read cached tile file")
            }
        }

        // Online fallback: download from OpenStreetMap
        if (layerId == "base") {
            try {
                val url = java.net.URL("https://basemaps.cartocdn.com/rastertiles/dark_all/$zoom/$x/$y.png")
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")

                if (connection.responseCode == 200) {
                    val bytes = connection.inputStream.readBytes()
                    val tile = MapTileModel(layerId, zoom, x, y, bytes)
                    synchronized(tileCache) {
                        tileCache[key] = tile
                    }
                    // Write to file system
                    file.parentFile?.mkdirs()
                    file.writeBytes(bytes)
                    return Result.Success(tile)
                }
            } catch (e: Exception) {
                timber.log.Timber.e(e, "Failed to download tile from OpenStreetMap")
            }
        }
        return Result.Error(Exception("Tile not found in cache and online fallback failed"))
    }

    override suspend fun clearLayer(layerId: String): Result<Unit> {
        return try {
            synchronized(tileCache) {
                tileCache.entries.removeIf { it.key.startsWith("$layerId:") }
            }
            val dir = File(context.cacheDir, "map_tiles/$layerId")
            if (dir.exists()) {
                dir.deleteRecursively()
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    private fun getTileFile(layerId: String, zoom: Int, x: Int, y: Int): File {
        val dir = File(context.cacheDir, "map_tiles/$layerId/$zoom/$x")
        return File(dir, "$y.png")
    }

    private fun countCachedTiles(): Int {
        val dir = File(context.cacheDir, "map_tiles")
        if (!dir.exists()) return 0
        return dir.walkTopDown().filter { it.isFile && it.extension == "png" }.count()
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
