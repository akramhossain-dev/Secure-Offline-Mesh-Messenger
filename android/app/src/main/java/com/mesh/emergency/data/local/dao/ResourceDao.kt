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
import com.mesh.emergency.data.local.entity.ResourceEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object regulating queries targeting the 'resources' table.
 */
@Dao
interface ResourceDao {

    /** Streams published supply items. */
    @Query("SELECT * FROM resources ORDER BY createdTime DESC")
    fun getResources(): Flow<List<ResourceEntity>>

    /** Paginated resource query with optional type filter (A33.3). */
    @Query("SELECT * FROM resources WHERE (:type IS NULL OR type = :type) ORDER BY createdTime DESC LIMIT :limit OFFSET :offset")
    suspend fun getPagedResources(type: String?, limit: Int, offset: Int): List<ResourceEntity>

    /** Get only available resources using index hint (A33.3). */
    @Query("SELECT * FROM resources WHERE availabilityStatus IN ('AVAILABLE', 'LIMITED') ORDER BY createdTime DESC")
    fun getAvailableResources(): Flow<List<ResourceEntity>>

    /** Insert or replace resource supply posts. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResource(resource: ResourceEntity)

    /** Delete resource supply posts. */
    @Delete
    suspend fun deleteResource(resource: ResourceEntity)
}
