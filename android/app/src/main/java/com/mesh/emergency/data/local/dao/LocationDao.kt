/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mesh.emergency.data.local.entity.LocationEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object regulating queries targeting the 'locations' table.
 */
@Dao
interface LocationDao {

    /** Streams location logs trace list for a specific contact user. */
    @Query("SELECT * FROM locations WHERE userId = :userId ORDER BY timestamp DESC")
    fun getLocationsForUser(userId: String): Flow<List<LocationEntity>>

    /** Streams all location logs traces. */
    @Query("SELECT * FROM locations ORDER BY timestamp DESC")
    fun getAllLocations(): Flow<List<LocationEntity>>

    /** Insert or replace location coordinates logs. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: LocationEntity)

    /** Delete location coordinates logs. */
    @Delete
    suspend fun deleteLocation(location: LocationEntity)
}
