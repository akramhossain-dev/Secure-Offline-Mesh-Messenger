/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.communication.forward

import com.mesh.emergency.core.common.result.Result
import com.mesh.emergency.core.communication.CommunicationManager
import com.mesh.emergency.core.communication.forward.ForwardingEngine
import com.mesh.emergency.core.communication.queue.MessageQueueManager
import com.mesh.emergency.data.local.LocalDataSource
import com.mesh.emergency.data.local.entity.DbDeliveryStatus
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [ForwardingEngine] managing backoff retry waits and TTL wipes.
 */
@Singleton
class ForwardingEngineImpl @Inject constructor(
    private val queueManager: MessageQueueManager,
    private val communicationManager: CommunicationManager,
    private val localDataSource: LocalDataSource
) : ForwardingEngine {

    private val MAX_RETRIES_LIMIT = 5

    override suspend fun processQueue() {
        val pendingMessages = queueManager.getPendingQueue().first()
        val now = System.currentTimeMillis()

        for (message in pendingMessages) {
            // 1. Check expiration (TTL Sweep)
            if (now > message.expiryTime) {
                queueManager.updateStatus(message.entityId, DbDeliveryStatus.EXPIRED)
                continue
            }

            // 2. Check retry backoff wait criteria
            val elapsed = now - message.timestamp
            val requiredWait = when (message.retryCount) {
                0    -> 0L
                1    -> 5000L      // 5 seconds
                2    -> 30000L     // 30 seconds
                else -> 300000L    // 5 minutes
            }

            if (elapsed < requiredWait) {
                continue // Skip, backoff wait interval has not elapsed yet
            }

            // 3. Select active connected transceiver and write
            val activeTransport = communicationManager.activeTransport.value
            if (activeTransport == null || communicationManager.getAvailableTransports().isEmpty()) {
                // If offline and max retries exceeded, mark failed
                if (message.retryCount >= MAX_RETRIES_LIMIT) {
                    queueManager.updateStatus(message.entityId, DbDeliveryStatus.FAILED)
                }
                continue
            }

            queueManager.updateStatus(message.entityId, DbDeliveryStatus.SENDING)
            val result = communicationManager.sendMessage(message.content.toByteArray())
            when (result) {
                is Result.Success -> {
                    queueManager.updateStatus(message.entityId, DbDeliveryStatus.DELIVERED)
                }
                is Result.Error -> {
                    queueManager.incrementRetry(message.entityId)
                    if (message.retryCount + 1 >= MAX_RETRIES_LIMIT) {
                        queueManager.updateStatus(message.entityId, DbDeliveryStatus.FAILED)
                    } else {
                        queueManager.updateStatus(message.entityId, DbDeliveryStatus.QUEUED)
                    }
                }
                is Result.Loading -> {
                    // Do nothing, await outcome
                }
            }
        }
    }

    override suspend fun cleanExpiredMessages() {
        val messages = localDataSource.getMessagesForConversation("recipient_id").first()
        val now = System.currentTimeMillis()

        for (message in messages) {
            if (now > message.expiryTime && message.deliveryStatus != DbDeliveryStatus.EXPIRED) {
                queueManager.updateStatus(message.entityId, DbDeliveryStatus.EXPIRED)
            }
        }
    }
}
