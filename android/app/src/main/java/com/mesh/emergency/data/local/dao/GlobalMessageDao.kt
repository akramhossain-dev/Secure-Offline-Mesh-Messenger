/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mesh.emergency.data.local.entity.GlobalMessageEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for global broadcast chat messages.
 *
 * Uses [OnConflictStrategy.IGNORE] on insert to silently deduplicate messages
 * that arrive multiple times (e.g., forwarded through multiple BLE hops).
 */
@Dao
interface GlobalMessageDao {

    /** Returns all global messages ordered chronologically as a live Flow. */
    @Query("SELECT * FROM global_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<GlobalMessageEntity>>

    /**
     * Inserts a global message. If a message with the same [GlobalMessageEntity.messageId]
     * already exists, the insert is silently ignored — guaranteeing exactly-once storage.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMessage(message: GlobalMessageEntity)

    /** Returns a single message by its UUID, or null if not present (used for dedup checks). */
    @Query("SELECT * FROM global_messages WHERE messageId = :id LIMIT 1")
    suspend fun getById(id: String): GlobalMessageEntity?

    /** Returns the 50 most recent messages for new-node sync payloads. */
    @Query("SELECT * FROM global_messages ORDER BY timestamp DESC LIMIT 50")
    suspend fun getRecent50(): List<GlobalMessageEntity>

    /** Updates delivery status for a single message by ID. */
    @Query("UPDATE global_messages SET deliveryStatus = :status WHERE messageId = :id")
    suspend fun updateDeliveryStatus(id: String, status: String)

    /** Updates any stuck SENDING messages to FAILED on app launch. */
    @Query("UPDATE global_messages SET deliveryStatus = 'FAILED' WHERE deliveryStatus = 'SENDING'")
    suspend fun failStuckMessages()

    /** Updates a global message log. */
    @androidx.room.Update
    suspend fun updateMessage(message: GlobalMessageEntity)

    /** Deletes a message by its ID. */
    @Query("DELETE FROM global_messages WHERE messageId = :id")
    suspend fun deleteMessage(id: String)
}
