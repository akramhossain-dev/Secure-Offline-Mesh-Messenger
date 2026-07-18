/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mesh.emergency.data.local.entity.VoiceMessageEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object regulating queries targeting the 'voice_messages' table.
 */
@Dao
interface VoiceMessageDao {

    @Query("SELECT * FROM voice_messages ORDER BY timestamp DESC")
    fun getVoiceMessages(): Flow<List<VoiceMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoiceMessage(voice: VoiceMessageEntity)

    @Query("DELETE FROM voice_messages WHERE entityId = :id")
    suspend fun deleteVoiceMessage(id: String)
}
