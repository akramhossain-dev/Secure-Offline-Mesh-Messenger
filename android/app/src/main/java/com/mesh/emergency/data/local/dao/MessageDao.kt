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
}
