/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.communication.queue

import com.mesh.emergency.core.communication.queue.MessageQueueManager
import com.mesh.emergency.data.local.LocalDataSource
import com.mesh.emergency.data.local.entity.DbDeliveryStatus
import com.mesh.emergency.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [MessageQueueManager] querying database lists using [LocalDataSource].
 */
@Singleton
class MessageQueueManagerImpl @Inject constructor(
    private val localDataSource: LocalDataSource
) : MessageQueueManager {

    override fun getPendingQueue(): Flow<List<MessageEntity>> {
        // Retrieve all messages marked PENDING or QUEUED
        return localDataSource.getMessagesForConversation("recipient_id").map { list ->
            list.filter {
                it.deliveryStatus == DbDeliveryStatus.PENDING ||
                        it.deliveryStatus == DbDeliveryStatus.QUEUED
            }
        }
    }

    override suspend fun enqueue(message: MessageEntity) {
        localDataSource.insertMessage(message)
    }

    override suspend fun updateStatus(messageId: String, status: DbDeliveryStatus) {
        val existing = localDataSource.getMessageById(messageId)
        if (existing != null) {
            val updated = existing.copy(deliveryStatus = status)
            localDataSource.insertMessage(updated)
        }
    }

    override suspend fun incrementRetry(messageId: String) {
        val existing = localDataSource.getMessageById(messageId)
        if (existing != null) {
            val updated = existing.copy(retryCount = existing.retryCount + 1)
            localDataSource.insertMessage(updated)
        }
    }
}
