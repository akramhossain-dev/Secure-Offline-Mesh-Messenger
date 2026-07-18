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
import com.mesh.emergency.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object regulating queries targeting the 'messages' table.
 */
@Dao
interface MessageDao {

    /** Streams messages matching a conversation. */
    @Query("SELECT * FROM messages WHERE conversationId = :convId ORDER BY timestamp ASC")
    fun getMessagesForConversation(convId: String): Flow<List<MessageEntity>>

    /** Queries a message log by primary ID. */
    @Query("SELECT * FROM messages WHERE entityId = :id LIMIT 1")
    suspend fun getMessageById(id: String): MessageEntity?

    /** Insert message log. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    /** Delete message log. */
    @Delete
    suspend fun deleteMessage(message: MessageEntity)

    /** Paginated message query for large conversations (A33.3). */
    @Query("SELECT * FROM messages WHERE conversationId = :convId ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getPagedMessages(convId: String, limit: Int, offset: Int): List<MessageEntity>

    /** Count total messages in a conversation. */
    @Query("SELECT COUNT(*) FROM messages WHERE conversationId = :convId")
    suspend fun countMessages(convId: String): Int

    /** Purges all message entries from storage (A34.8). */
    @Query("DELETE FROM messages")
    suspend fun deleteAllMessages()
}
