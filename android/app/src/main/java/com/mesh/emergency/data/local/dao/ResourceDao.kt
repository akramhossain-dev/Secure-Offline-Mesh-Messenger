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
    @Query("SELECT * FROM resources ORDER BY timestamp DESC")
    fun getResources(): Flow<List<ResourceEntity>>

    /** Insert or replace resource supply posts. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResource(resource: ResourceEntity)

    /** Delete resource supply posts. */
    @Delete
    suspend fun deleteResource(resource: ResourceEntity)
}
