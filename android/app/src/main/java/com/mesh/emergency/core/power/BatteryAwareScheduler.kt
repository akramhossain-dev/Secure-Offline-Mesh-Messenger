/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.power

import com.mesh.emergency.core.common.result.Result
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Battery-aware task scheduler that gates background operations
 * based on the active [PowerSavingMode].
 *
 * Prevents expensive operations (BLE scans, DB queries) when battery is low.
 * Integrates with [PowerManager] (A19) for mode awareness.
 */
@Singleton
class BatteryAwareScheduler @Inject constructor(
    private val powerManager: PowerManager
) {

    /** Returns true if heavy-duty operations are permitted given battery state. */
    val isHeavyOperationAllowed: Boolean
        get() = powerManager.currentMode.value != PowerSavingMode.ULTRA_SAVE

    /** Returns true if BLE scanning is permitted given battery state. */
    val isScanningAllowed: Boolean
        get() = powerManager.currentMode.value != PowerSavingMode.ULTRA_SAVE &&
                powerManager.currentMode.value != PowerSavingMode.AGGRESSIVE_SAVE

    /** Returns BLE scan interval in ms based on current power mode. */
    val bleScanIntervalMs: Long
        get() = when (powerManager.currentMode.value) {
            PowerSavingMode.PERFORMANCE     -> BLE_INTERVAL_PERFORMANCE
            PowerSavingMode.BALANCED        -> BLE_INTERVAL_BALANCED
            PowerSavingMode.AGGRESSIVE_SAVE -> BLE_INTERVAL_AGGRESSIVE
            PowerSavingMode.ULTRA_SAVE      -> Long.MAX_VALUE // scanning disabled
        }

    /** Returns location update interval in ms based on power mode. */
    val locationIntervalMs: Long
        get() = when (powerManager.currentMode.value) {
            PowerSavingMode.PERFORMANCE     -> LOCATION_INTERVAL_PERFORMANCE
            PowerSavingMode.BALANCED        -> LOCATION_INTERVAL_BALANCED
            PowerSavingMode.AGGRESSIVE_SAVE -> LOCATION_INTERVAL_AGGRESSIVE
            PowerSavingMode.ULTRA_SAVE      -> Long.MAX_VALUE
        }

    /** Returns database batch size based on power mode. */
    val dbBatchSize: Int
        get() = when (powerManager.currentMode.value) {
            PowerSavingMode.PERFORMANCE     -> DB_BATCH_LARGE
            PowerSavingMode.BALANCED        -> DB_BATCH_MEDIUM
            PowerSavingMode.AGGRESSIVE_SAVE -> DB_BATCH_SMALL
            PowerSavingMode.ULTRA_SAVE      -> DB_BATCH_MINIMAL
        }

    /**
     * Executes [block] only if battery conditions allow heavy operations.
     * Returns a skipped result otherwise.
     */
    suspend fun <T> runIfAllowed(block: suspend () -> Result<T>): Result<T> {
        return if (isHeavyOperationAllowed) {
            block()
        } else {
            Timber.d("BatteryScheduler: Skipping heavy operation (mode=${powerManager.currentMode.value})")
            Result.Error(Exception("Operation skipped — battery saver active"))
        }
    }

    companion object {
        const val BLE_INTERVAL_PERFORMANCE  = 5_000L   // 5 seconds
        const val BLE_INTERVAL_BALANCED     = 15_000L  // 15 seconds
        const val BLE_INTERVAL_AGGRESSIVE   = 60_000L  // 1 minute

        const val LOCATION_INTERVAL_PERFORMANCE  = 10_000L  // 10 seconds
        const val LOCATION_INTERVAL_BALANCED     = 30_000L  // 30 seconds
        const val LOCATION_INTERVAL_AGGRESSIVE   = 120_000L // 2 minutes

        const val DB_BATCH_LARGE   = 100
        const val DB_BATCH_MEDIUM  = 50
        const val DB_BATCH_SMALL   = 20
        const val DB_BATCH_MINIMAL = 10
    }
}
