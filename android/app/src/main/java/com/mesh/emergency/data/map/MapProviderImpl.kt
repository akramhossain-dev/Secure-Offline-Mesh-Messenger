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
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation stub of [MapProvider] wrapping offline vector maps.
 */
@Singleton
class MapProviderImpl @Inject constructor() : MapProvider {

    private val _mapState = MutableStateFlow(MapState.EMPTY)
    override val mapState: StateFlow<MapState> = _mapState.asStateFlow()

    override fun loadOfflineMap(filePath: String): Result<Unit> {
        _mapState.value = MapState.LOADING
        return if (filePath.isNotBlank()) {
            _mapState.value = MapState.LOADED
            Result.Success(Unit)
        } else {
            _mapState.value = MapState.ERROR
            Result.Error(Exception("Invalid map file path"))
        }
    }

    override fun clearMapCache() {
        _mapState.value = MapState.EMPTY
    }

    override fun getAvailableLayers(): List<String> {
        return listOf("topo_layer", "rescue_nodes_layer", "sos_markers_layer")
    }
}
