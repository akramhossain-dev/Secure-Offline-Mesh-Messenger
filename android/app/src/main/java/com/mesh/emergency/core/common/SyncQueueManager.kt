/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.common

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.util.LinkedList
import java.util.Queue
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory sync queue manager for offline operations.
 *
 * Operations are queued when no mesh connection is available and
 * dequeued when connectivity is restored. Capacity capped at [MAX_QUEUE_SIZE].
 */
@Singleton
class SyncQueueManager @Inject constructor(
    private val offlineStatusManager: OfflineStatusManager
) {

    private val queue: Queue<SyncOperation> = LinkedList()

    private val _queueSize = MutableStateFlow(0)
    val queueSize: StateFlow<Int> = _queueSize.asStateFlow()

    /**
     * Enqueues an operation for deferred delivery.
     * Returns false if queue is full.
     */
    fun enqueue(operation: SyncOperation): Boolean {
        if (queue.size >= MAX_QUEUE_SIZE) {
            Timber.w("SyncQueue: Queue full (${MAX_QUEUE_SIZE}), dropping operation: ${operation.id}")
            return false
        }
        queue.offer(operation)
        _queueSize.value = queue.size
        offlineStatusManager.enqueue()
        Timber.d("SyncQueue: Enqueued ${operation.type} (queue size: ${queue.size})")
        return true
    }

    /** Dequeues the next pending operation, or null if empty. */
    fun dequeueNext(): SyncOperation? {
        val op = queue.poll()
        if (op != null) {
            _queueSize.value = queue.size
            offlineStatusManager.dequeue()
        }
        return op
    }

    /** Clears all pending operations (e.g., after successful bulk sync). */
    fun clearAll() {
        val count = queue.size
        queue.clear()
        _queueSize.value = 0
        offlineStatusManager.resetQueue()
        Timber.d("SyncQueue: Cleared $count operations")
    }

    /** Returns a snapshot of pending operations. */
    fun peekAll(): List<SyncOperation> = queue.toList()

    fun isEmpty(): Boolean = queue.isEmpty()

    companion object {
        const val MAX_QUEUE_SIZE = 500
    }
}

/**
 * Represents a deferred sync operation.
 */
data class SyncOperation(
    val id: String,
    val type: SyncOperationType,
    val payload: ByteArray,
    val targetNodeId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SyncOperation) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

enum class SyncOperationType {
    MESSAGE,
    LOCATION,
    RESOURCE,
    EMERGENCY,
    ACK
}
