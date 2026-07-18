/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.communication.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mesh.emergency.core.communication.forward.ForwardingEngine
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Background worker executing queue routing sweeps.
 */
@HiltWorker
class QueueProcessWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val forwardingEngine: ForwardingEngine
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            forwardingEngine.processQueue()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
