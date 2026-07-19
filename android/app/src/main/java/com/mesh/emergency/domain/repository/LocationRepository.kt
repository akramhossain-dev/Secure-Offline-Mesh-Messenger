/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.domain.repository

import com.mesh.emergency.core.common.result.Result
import com.mesh.emergency.core.utils.LocationData
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface regulating device location histories persistence.
 */
interface LocationRepository {
    /** Streams saved coordinates log lists. */
    fun getSavedLocations(userId: String): Flow<Result<List<LocationData>>>

    /** Streams all saved coordinates logs in the system. */
    fun getAllLocations(): Flow<Result<List<LocationData>>>

    /** Persists a coordinates point entry in local database. */
    suspend fun saveLocation(location: LocationData): Result<Unit>

    /** Clears all coordinates entries matching a user identifier. */
    suspend fun deleteLocationHistory(userId: String): Result<Unit>
}
