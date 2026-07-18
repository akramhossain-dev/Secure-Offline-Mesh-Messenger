/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency

import com.mesh.emergency.core.map.MapBounds
import com.mesh.emergency.core.map.MapLayerModel
import com.mesh.emergency.core.map.MapLayerType
import com.mesh.emergency.core.map.MapTileModel
import com.mesh.emergency.data.map.MapRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for offline map foundation (A32.1).
 *
 * Tests:
 * - Default layers are loaded correctly
 * - Tile save and cache eviction
 * - Layer visibility toggle
 * - Map bounds retrieval
 * - Offline data snapshot
 */
class OfflineMapTest {

    private lateinit var mapRepository: MapRepositoryImpl

    @Before
    fun setup() {
        mapRepository = MapRepositoryImpl()
    }

    @Test
    fun `default layers include base, roads, nodes, emergency, elevation`() = runTest {
        val result = mapRepository.getMapLayers().first()
        assertTrue(result is com.mesh.emergency.core.common.result.Result.Success)
        val layers = (result as com.mesh.emergency.core.common.result.Result.Success).data
        assertEquals(5, layers.size)
        assertTrue(layers.any { it.id == "base" && it.type == MapLayerType.BASE })
        assertTrue(layers.any { it.id == "roads" && it.type == MapLayerType.ROADS })
        assertTrue(layers.any { it.id == "nodes" && it.type == MapLayerType.NODES })
        assertTrue(layers.any { it.id == "emergency" && it.type == MapLayerType.EMERGENCY_ZONES })
        assertTrue(layers.any { it.id == "elevation" && it.type == MapLayerType.ELEVATION })
    }

    @Test
    fun `elevation layer is not visible by default`() = runTest {
        val result = mapRepository.getMapLayers().first()
        val layers = (result as com.mesh.emergency.core.common.result.Result.Success).data
        val elevation = layers.first { it.id == "elevation" }
        assertTrue(!elevation.isVisible)
    }

    @Test
    fun `save tile stores in cache`() = runTest {
        val tile = MapTileModel(
            layerId = "base",
            zoom = 10, x = 1, y = 1,
            data = byteArrayOf(1, 2, 3)
        )
        val result = mapRepository.saveMapTile(tile)
        assertTrue(result is com.mesh.emergency.core.common.result.Result.Success)

        val offlineData = mapRepository.getOfflineMapData()
        val data = (offlineData as com.mesh.emergency.core.common.result.Result.Success).data
        assertEquals(1, data.tileCount)
    }

    @Test
    fun `clear layer removes associated tiles`() = runTest {
        repeat(3) { i ->
            mapRepository.saveMapTile(MapTileModel("base", 10, i, i, byteArrayOf()))
        }
        mapRepository.clearLayer("base")

        val data = (mapRepository.getOfflineMapData() as com.mesh.emergency.core.common.result.Result.Success).data
        assertEquals(0, data.tileCount)
    }

    @Test
    fun `toggle layer visibility updates state`() = runTest {
        mapRepository.updateLayerVisibility("elevation", true)
        val result = mapRepository.getMapLayers().first()
        val layers = (result as com.mesh.emergency.core.common.result.Result.Success).data
        val elevation = layers.first { it.id == "elevation" }
        assertTrue(elevation.isVisible)
    }

    @Test
    fun `getMapBounds returns valid default bounds`() = runTest {
        val result = mapRepository.getMapBounds()
        val bounds = (result as com.mesh.emergency.core.common.result.Result.Success).data
        assertEquals(MapBounds(), bounds)
    }
}
