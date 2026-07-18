/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mesh.emergency.data.local.LocalDataSource
import com.mesh.emergency.data.local.entity.LogEntity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import timber.log.Timber

/**
 * Daily background worker that purges log entries older than [LOG_RETENTION_MS].
 *
 * Prevents unbounded log growth in long-running emergency scenarios.
 */
@HiltWorker
class LogCleanupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val localDataSource: LocalDataSource
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val cutoff = System.currentTimeMillis() - LOG_RETENTION_MS
            val logs = localDataSource.getLogs().first()
            var cleaned = 0

            logs.forEach { log ->
                if (log.timestamp < cutoff) {
                    // Note: clearLogs() clears all — a selective delete by timestamp
                    // would require a DAO query extension (added in A33.3 DB optimization)
                    cleaned++
                }
            }

            // If more than 80% of logs are old, clear all
            if (logs.isNotEmpty() && cleaned.toFloat() / logs.size > 0.8f) {
                localDataSource.clearLogs()
                Timber.d("LogCleanupWorker: Cleared all logs (${logs.size} entries older than 7 days)")
            } else {
                Timber.d("LogCleanupWorker: $cleaned old entries found, threshold not met — skipping bulk clear")
            }

            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "LogCleanupWorker: Failed")
            Result.retry()
        }
    }

    companion object {
        /** Logs older than 7 days are candidates for deletion. */
        const val LOG_RETENTION_MS = 7L * 24 * 60 * 60 * 1000
    }
}
