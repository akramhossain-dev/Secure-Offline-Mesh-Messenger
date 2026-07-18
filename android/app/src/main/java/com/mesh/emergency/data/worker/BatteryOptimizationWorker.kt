/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mesh.emergency.core.power.PowerManager
import com.mesh.emergency.core.power.PowerSavingMode
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

/**
 * Background worker that checks battery conditions and defers non-critical tasks.
 *
 * Runs every 30 minutes to evaluate current power mode and log recommendations.
 * Integrates with [PowerManager] (A19).
 */
@HiltWorker
class BatteryOptimizationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val powerManager: PowerManager
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val currentMode = powerManager.currentMode.value
            Timber.d("BatteryOptimizationWorker: Current power mode = $currentMode")

            when (currentMode) {
                PowerSavingMode.ULTRA_SAVE -> {
                    Timber.w("BatteryOptimizationWorker: ULTRA_SAVE — all non-critical tasks deferred")
                }
                PowerSavingMode.AGGRESSIVE_SAVE -> {
                    Timber.d("BatteryOptimizationWorker: AGGRESSIVE_SAVE — reducing scan frequency")
                }
                PowerSavingMode.BALANCED -> {
                    Timber.d("BatteryOptimizationWorker: BALANCED — standard operation")
                }
                PowerSavingMode.PERFORMANCE -> {
                    Timber.d("BatteryOptimizationWorker: PERFORMANCE — full operation")
                }
            }

            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "BatteryOptimizationWorker: Error")
            Result.failure()
        }
    }
}
