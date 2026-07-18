/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mesh.emergency.core.common.result.Result
import com.mesh.emergency.core.resource.ResourceManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

/**
 * Periodic background worker that expires resources whose TTL has elapsed.
 *
 * Runs every 6 hours. Integrates with [ResourceManager] (A17).
 */
@HiltWorker
class ResourceExpiryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val resourceManager: ResourceManager
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            Timber.d("ResourceExpiryWorker: Starting expiry check")
            when (val result = resourceManager.expireResources()) {
                is com.mesh.emergency.core.common.result.Result.Success -> {
                    Timber.d("ResourceExpiryWorker: Expiry check complete")
                    Result.success()
                }
                is com.mesh.emergency.core.common.result.Result.Error -> {
                    Timber.e(result.exception, "ResourceExpiryWorker: Expiry failed")
                    Result.retry()
                }
                else -> Result.success()
            }
        } catch (e: Exception) {
            Timber.e(e, "ResourceExpiryWorker: Unexpected error")
            Result.retry()
        }
    }
}
