/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.map

import com.mesh.emergency.core.common.result.Result
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface contract representing offline map loading systems.
 */
interface MapProvider {
    /** Exposes active mapping state. */
    val mapState: StateFlow<MapState>

    /** Loads raw map files offline. */
    fun loadOfflineMap(filePath: String): Result<Unit>

    /** Wipes mapped memory cache. */
    fun clearMapCache()

    /** Exposes layer names available. */
    fun getAvailableLayers(): List<String>
}

/**
 * Mapped cache state checkpoints.
 */
enum class MapState {
    LOADED,
    LOADING,
    ERROR,
    EMPTY
}
