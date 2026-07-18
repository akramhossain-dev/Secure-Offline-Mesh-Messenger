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
import com.mesh.emergency.data.local.entity.DeviceEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object regulating queries targeting the 'devices' table.
 */
@Dao
interface DeviceDao {

    /** Stream list of discovered BLE hardware devices. */
    @Query("SELECT * FROM devices ORDER BY lastSeen DESC")
    fun getDevices(): Flow<List<DeviceEntity>>

    /** Clear cached scanned devices list. */
    @Query("DELETE FROM devices")
    suspend fun clearAllDevices()

    /** Insert or replace scanned device logs. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: DeviceEntity)

    /** Delete scanned device log. */
    @Delete
    suspend fun deleteDevice(device: DeviceEntity)
}
