/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mesh.emergency.core.common.SyncQueueManager
import com.mesh.emergency.core.common.SyncOperationType
import com.mesh.emergency.core.communication.CommunicationManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

/**
 * Periodic background worker that processes the offline sync queue.
 *
 * Drains pending [SyncOperation]s when mesh connectivity constraints are met.
 * Runs every 15 minutes with battery-not-low constraint.
 */
@HiltWorker
class SyncQueueWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncQueueManager: SyncQueueManager,
    private val communicationManager: CommunicationManager
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        if (syncQueueManager.isEmpty()) {
            Timber.d("SyncQueueWorker: Queue empty, nothing to process")
            return Result.success()
        }

        val startSize = syncQueueManager.queueSize.value
        var processed = 0
        var failed = 0

        try {
            while (!syncQueueManager.isEmpty()) {
                val operation = syncQueueManager.dequeueNext() ?: break

                try {
                    when (operation.type) {
                        SyncOperationType.MESSAGE,
                        SyncOperationType.LOCATION,
                        SyncOperationType.RESOURCE,
                        SyncOperationType.EMERGENCY,
                        SyncOperationType.ACK -> {
                            val deliveryResult = communicationManager.sendMessage(operation.payload)
                            if (deliveryResult is com.mesh.emergency.core.common.result.Result.Success) {
                                processed++
                            } else {
                                throw Exception("Delivery failed: $deliveryResult")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Timber.w(e, "SyncQueueWorker: Failed to process operation ${operation.id}")
                    // Re-enqueue with retry limit
                    if (operation.retryCount < MAX_RETRIES) {
                        syncQueueManager.enqueue(operation.copy(retryCount = operation.retryCount + 1))
                    } else {
                        Timber.e("SyncQueueWorker: Dropping operation ${operation.id} after $MAX_RETRIES retries")
                    }
                    failed++
                }
            }

            Timber.d("SyncQueueWorker: Processed $processed/$startSize, failed=$failed")
            return Result.success()
        } catch (e: Exception) {
            Timber.e(e, "SyncQueueWorker: Unexpected failure")
            return Result.retry()
        }
    }

    companion object {
        const val MAX_RETRIES = 3
    }
}
