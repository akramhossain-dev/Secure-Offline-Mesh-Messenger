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
import com.mesh.emergency.data.local.entity.EmergencyEventEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object regulating queries targeting the 'emergency_events' table.
 */
@Dao
interface EmergencyEventDao {

    /** Streams distress emergency alerts. */
    @Query("SELECT * FROM emergency_events ORDER BY timestamp DESC")
    fun getEmergencyEvents(): Flow<List<EmergencyEventEntity>>

    /** Queries a single emergency event by its primary key. */
    @Query("SELECT * FROM emergency_events WHERE entityId = :id LIMIT 1")
    suspend fun getEmergencyEventById(id: String): EmergencyEventEntity?

    /** Insert or replace distress alerts logs. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmergencyEvent(event: EmergencyEventEntity)

    /** Delete distress alerts logs. */
    @Delete
    suspend fun deleteEmergencyEvent(event: EmergencyEventEntity)
}
