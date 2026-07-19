/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.system

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.mesh.emergency.data.worker.BatteryOptimizationWorker
import com.mesh.emergency.data.worker.LogCleanupWorker
import com.mesh.emergency.data.worker.ResourceExpiryWorker
import com.mesh.emergency.data.worker.SyncQueueWorker
import com.mesh.emergency.core.communication.worker.QueueProcessWorker
import com.mesh.emergency.core.communication.worker.CleanupWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized WorkManager coordinator for all background tasks.
 *
 * All workers use [Constraints] to protect battery and storage.
 * Jobs are idempotent and safely restartable.
 */
@Singleton
class MeshWorkManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val workManager: WorkManager by lazy { WorkManager.getInstance(context) }

    /** Common constraints: not low battery, not low storage. */
    private val defaultConstraints = Constraints.Builder()
        .setRequiresBatteryNotLow(true)
        .setRequiresStorageNotLow(true)
        .build()

    /** Relaxed constraints for non-critical cleanup (runs even on low battery). */
    private val relaxedConstraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
        .build()

    /**
     * Registers all periodic background workers.
     * Safe to call multiple times — uses [ExistingPeriodicWorkPolicy.KEEP].
     */
    fun scheduleAllWorkers() {
        scheduleResourceExpiry()
        scheduleLogCleanup()
        scheduleSyncQueue()
        scheduleBatteryOptimization()
        scheduleQueueProcess()
        scheduleCleanup()
        Timber.d("MeshWorkManager: All workers scheduled")
    }

    /** Resource expiry check every 6 hours. */
    private fun scheduleResourceExpiry() {
        val request = PeriodicWorkRequestBuilder<ResourceExpiryWorker>(6, TimeUnit.HOURS)
            .setConstraints(defaultConstraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
            .addTag(TAG_RESOURCE_EXPIRY)
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_RESOURCE_EXPIRY,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
        Timber.d("MeshWorkManager: ResourceExpiry worker scheduled (6h)")
    }

    /** Log cleanup once per day. */
    private fun scheduleLogCleanup() {
        val request = PeriodicWorkRequestBuilder<LogCleanupWorker>(1, TimeUnit.DAYS)
            .setConstraints(relaxedConstraints)
            .setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.MINUTES)
            .addTag(TAG_LOG_CLEANUP)
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_LOG_CLEANUP,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
        Timber.d("MeshWorkManager: LogCleanup worker scheduled (24h)")
    }

    /** Sync queue processor every 15 minutes. */
    private fun scheduleSyncQueue() {
        val request = PeriodicWorkRequestBuilder<SyncQueueWorker>(15, TimeUnit.MINUTES)
            .setConstraints(defaultConstraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES)
            .addTag(TAG_SYNC_QUEUE)
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_SYNC_QUEUE,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
        Timber.d("MeshWorkManager: SyncQueue worker scheduled (15m)")
    }

    /** Battery optimization check every 30 minutes. */
    private fun scheduleBatteryOptimization() {
        val request = PeriodicWorkRequestBuilder<BatteryOptimizationWorker>(30, TimeUnit.MINUTES)
            .setConstraints(relaxedConstraints)
            .addTag(TAG_BATTERY_OPT)
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_BATTERY_OPT,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
        Timber.d("MeshWorkManager: BatteryOptimization worker scheduled (30m)")
    }

    /** Queue routing processor every 15 minutes. */
    private fun scheduleQueueProcess() {
        val request = PeriodicWorkRequestBuilder<QueueProcessWorker>(15, TimeUnit.MINUTES)
            .setConstraints(defaultConstraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES)
            .addTag(TAG_QUEUE_PROCESS)
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_QUEUE_PROCESS,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
        Timber.d("MeshWorkManager: QueueProcess worker scheduled (15m)")
    }

    /** Message cleanup sweep every 6 hours. */
    private fun scheduleCleanup() {
        val request = PeriodicWorkRequestBuilder<CleanupWorker>(6, TimeUnit.HOURS)
            .setConstraints(defaultConstraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
            .addTag(TAG_CLEANUP)
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_CLEANUP,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
        Timber.d("MeshWorkManager: Cleanup worker scheduled (6h)")
    }

    /** Cancels all managed workers. */
    fun cancelAllWorkers() {
        workManager.cancelAllWorkByTag(TAG_RESOURCE_EXPIRY)
        workManager.cancelAllWorkByTag(TAG_LOG_CLEANUP)
        workManager.cancelAllWorkByTag(TAG_SYNC_QUEUE)
        workManager.cancelAllWorkByTag(TAG_BATTERY_OPT)
        workManager.cancelAllWorkByTag(TAG_QUEUE_PROCESS)
        workManager.cancelAllWorkByTag(TAG_CLEANUP)
        Timber.d("MeshWorkManager: All workers cancelled")
    }

    companion object {
        const val WORK_RESOURCE_EXPIRY = "mesh_resource_expiry"
        const val WORK_LOG_CLEANUP     = "mesh_log_cleanup"
        const val WORK_SYNC_QUEUE      = "mesh_sync_queue"
        const val WORK_BATTERY_OPT     = "mesh_battery_opt"
        const val WORK_QUEUE_PROCESS   = "mesh_queue_process"
        const val WORK_CLEANUP         = "mesh_cleanup"

        const val TAG_RESOURCE_EXPIRY  = "resource_expiry"
        const val TAG_LOG_CLEANUP      = "log_cleanup"
        const val TAG_SYNC_QUEUE       = "sync_queue"
        const val TAG_BATTERY_OPT      = "battery_opt"
        const val TAG_QUEUE_PROCESS    = "queue_process"
        const val TAG_CLEANUP          = "cleanup"
    }
}
