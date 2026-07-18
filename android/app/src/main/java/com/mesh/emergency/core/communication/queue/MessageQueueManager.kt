/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.communication.queue

import com.mesh.emergency.data.local.entity.DbDeliveryStatus
import com.mesh.emergency.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

/**
 * Interface contract managing local database pending queues states.
 */
interface MessageQueueManager {
    /** streams lists of messages waiting for transmission. */
    fun getPendingQueue(): Flow<List<MessageEntity>>

    /** Enqueues a message for transmission attempts. */
    suspend fun enqueue(message: MessageEntity)

    /** Updates status tags on message identifiers. */
    suspend fun updateStatus(messageId: String, status: DbDeliveryStatus)

    /** Increments transmission attempt count. */
    suspend fun incrementRetry(messageId: String)
}
