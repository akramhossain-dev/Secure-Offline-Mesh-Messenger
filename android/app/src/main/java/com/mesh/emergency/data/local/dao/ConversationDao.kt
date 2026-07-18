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
import com.mesh.emergency.data.local.entity.ConversationEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object regulating queries targeting the 'conversations' table.
 */
@Dao
interface ConversationDao {

    /** Stream of active conversation listings. */
    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    fun getConversations(): Flow<List<ConversationEntity>>

    /** Queries conversation record by identifier. */
    @Query("SELECT * FROM conversations WHERE entityId = :id LIMIT 1")
    suspend fun getConversationById(id: String): ConversationEntity?

    /** Insert or update conversation. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity)

    /** Delete conversation. */
    @Delete
    suspend fun deleteConversation(conversation: ConversationEntity)
}
