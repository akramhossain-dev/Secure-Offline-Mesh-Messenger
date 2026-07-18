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
import com.mesh.emergency.data.local.entity.DeliveryStatusEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object regulating queries targeting the 'delivery_statuses' table.
 */
@Dao
interface DeliveryStatusDao {

    /** Streams validation check history logs matching a message identifier. */
    @Query("SELECT * FROM delivery_statuses WHERE messageId = :msgId")
    fun getDeliveryStatusesForMessage(msgId: String): Flow<List<DeliveryStatusEntity>>

    /** Insert or replace validation check log. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeliveryStatus(status: DeliveryStatusEntity)

    /** Delete validation check log. */
    @Delete
    suspend fun deleteDeliveryStatus(status: DeliveryStatusEntity)
}
