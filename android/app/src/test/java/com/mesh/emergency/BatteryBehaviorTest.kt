/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency

import com.mesh.emergency.core.power.BatteryAwareScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import com.mesh.emergency.core.power.PowerManager
import com.mesh.emergency.core.power.PowerSavingMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest

/**
 * Unit tests for BatteryAwareScheduler (A33.2).
 *
 * Tests:
 * - Scanning allowed in PERFORMANCE mode
 * - Scanning blocked in ULTRA_SAVE mode
 * - Correct BLE interval per mode
 * - Heavy operation gating
 */
class BatteryBehaviorTest {

    private fun makeScheduler(mode: PowerSavingMode): BatteryAwareScheduler {
        val powerManager = mock<PowerManager>()
        whenever(powerManager.currentMode).thenReturn(MutableStateFlow(mode))
        return BatteryAwareScheduler(powerManager)
    }

    @Test
    fun `PERFORMANCE mode allows scanning and heavy operations`() {
        val scheduler = makeScheduler(PowerSavingMode.PERFORMANCE)
        assertTrue(scheduler.isScanningAllowed)
        assertTrue(scheduler.isHeavyOperationAllowed)
    }

    @Test
    fun `BALANCED mode allows scanning and heavy operations`() {
        val scheduler = makeScheduler(PowerSavingMode.BALANCED)
        assertTrue(scheduler.isScanningAllowed)
        assertTrue(scheduler.isHeavyOperationAllowed)
    }

    @Test
    fun `AGGRESSIVE_SAVE mode blocks scanning but allows heavy ops`() {
        val scheduler = makeScheduler(PowerSavingMode.AGGRESSIVE_SAVE)
        assertFalse(scheduler.isScanningAllowed)
        assertTrue(scheduler.isHeavyOperationAllowed)
    }

    @Test
    fun `ULTRA_SAVE mode blocks scanning and heavy operations`() {
        val scheduler = makeScheduler(PowerSavingMode.ULTRA_SAVE)
        assertFalse(scheduler.isScanningAllowed)
        assertFalse(scheduler.isHeavyOperationAllowed)
    }

    @Test
    fun `BLE interval is shortest in PERFORMANCE mode`() {
        val scheduler = makeScheduler(PowerSavingMode.PERFORMANCE)
        assertEquals(BatteryAwareScheduler.BLE_INTERVAL_PERFORMANCE, scheduler.bleScanIntervalMs)
    }

    @Test
    fun `BLE interval is max in ULTRA_SAVE mode`() {
        val scheduler = makeScheduler(PowerSavingMode.ULTRA_SAVE)
        assertEquals(Long.MAX_VALUE, scheduler.bleScanIntervalMs)
    }

    @Test
    fun `DB batch size is largest in PERFORMANCE mode`() {
        val scheduler = makeScheduler(PowerSavingMode.PERFORMANCE)
        assertEquals(BatteryAwareScheduler.DB_BATCH_LARGE, scheduler.dbBatchSize)
    }

    @Test
    fun `DB batch size is smallest in ULTRA_SAVE mode`() {
        val scheduler = makeScheduler(PowerSavingMode.ULTRA_SAVE)
        assertEquals(BatteryAwareScheduler.DB_BATCH_MINIMAL, scheduler.dbBatchSize)
    }

    @Test
    fun `runIfAllowed returns error when heavy ops blocked`() = runTest {
        val scheduler = makeScheduler(PowerSavingMode.ULTRA_SAVE)
        var blockCalled = false
        val result = scheduler.runIfAllowed<Unit> {
            blockCalled = true
            com.mesh.emergency.core.common.result.Result.Success(Unit)
        }
        assertFalse(blockCalled)
        assertTrue(result is com.mesh.emergency.core.common.result.Result.Error)
    }

    @Test
    fun `runIfAllowed executes block in PERFORMANCE mode`() = runTest {
        val scheduler = makeScheduler(PowerSavingMode.PERFORMANCE)
        var blockCalled = false
        val result = scheduler.runIfAllowed<Unit> {
            blockCalled = true
            com.mesh.emergency.core.common.result.Result.Success(Unit)
        }
        assertTrue(blockCalled)
        assertTrue(result is com.mesh.emergency.core.common.result.Result.Success)
    }
}
