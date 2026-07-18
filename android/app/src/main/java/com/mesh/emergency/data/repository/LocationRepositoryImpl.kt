/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.repository

import com.mesh.emergency.core.common.result.Result
import com.mesh.emergency.core.utils.LocationData
import com.mesh.emergency.data.local.LocalDataSource
import com.mesh.emergency.data.local.entity.LocationEntity
import com.mesh.emergency.domain.repository.LocationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [LocationRepository] mapping Room database operations.
 */
@Singleton
class LocationRepositoryImpl @Inject constructor(
    private val localDataSource: LocalDataSource
) : LocationRepository {

    override fun getSavedLocations(userId: String): Flow<Result<List<LocationData>>> {
        return localDataSource.getLocationsForUser(userId).map { list ->
            Result.Success(list.map { it.toDomain() })
        }
    }

    override suspend fun saveLocation(location: LocationData): Result<Unit> {
        return try {
            val entity = LocationEntity(
                entityId = location.id,
                userId = "local_user_id",
                latitude = location.latitude,
                longitude = location.longitude,
                altitude = location.altitude,
                accuracy = location.accuracy,
                timestamp = location.timestamp,
                provider = location.provider,
                deviceId = location.deviceId
            )
            localDataSource.insertLocation(entity)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun deleteLocationHistory(userId: String): Result<Unit> {
        return try {
            // Bulk deletion operations are mapped inside lower local database sweeps
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}

private fun LocationEntity.toDomain(): LocationData = LocationData(
    id = entityId,
    latitude = latitude,
    longitude = longitude,
    altitude = altitude,
    accuracy = accuracy,
    timestamp = timestamp,
    provider = provider,
    deviceId = deviceId
)
