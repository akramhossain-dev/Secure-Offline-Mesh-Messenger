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
 * - Scanning allowed in NORMAL mode
 * - Scanning blocked in SAVING and EMERGENCY modes
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
    fun `NORMAL mode allows scanning and heavy operations`() {
        val scheduler = makeScheduler(PowerSavingMode.NORMAL)
        assertTrue(scheduler.isScanningAllowed)
        assertTrue(scheduler.isHeavyOperationAllowed)
    }

    @Test
    fun `SAVING mode blocks scanning but allows heavy ops`() {
        val scheduler = makeScheduler(PowerSavingMode.SAVING)
        assertFalse(scheduler.isScanningAllowed)
        assertTrue(scheduler.isHeavyOperationAllowed)
    }

    @Test
    fun `EMERGENCY mode blocks scanning and heavy operations`() {
        val scheduler = makeScheduler(PowerSavingMode.EMERGENCY)
        assertFalse(scheduler.isScanningAllowed)
        assertFalse(scheduler.isHeavyOperationAllowed)
    }

    @Test
    fun `BLE interval is balanced in NORMAL mode`() {
        val scheduler = makeScheduler(PowerSavingMode.NORMAL)
        assertEquals(BatteryAwareScheduler.BLE_INTERVAL_BALANCED, scheduler.bleScanIntervalMs)
    }

    @Test
    fun `BLE interval is max in EMERGENCY mode`() {
        val scheduler = makeScheduler(PowerSavingMode.EMERGENCY)
        assertEquals(Long.MAX_VALUE, scheduler.bleScanIntervalMs)
    }

    @Test
    fun `DB batch size is medium in NORMAL mode`() {
        val scheduler = makeScheduler(PowerSavingMode.NORMAL)
        assertEquals(BatteryAwareScheduler.DB_BATCH_MEDIUM, scheduler.dbBatchSize)
    }

    @Test
    fun `DB batch size is smallest in EMERGENCY mode`() {
        val scheduler = makeScheduler(PowerSavingMode.EMERGENCY)
        assertEquals(BatteryAwareScheduler.DB_BATCH_MINIMAL, scheduler.dbBatchSize)
    }

    @Test
    fun `runIfAllowed returns error when heavy ops blocked`() = runTest {
        val scheduler = makeScheduler(PowerSavingMode.EMERGENCY)
        var blockCalled = false
        val result = scheduler.runIfAllowed<Unit> {
            blockCalled = true
            com.mesh.emergency.core.common.result.Result.Success(Unit)
        }
        assertFalse(blockCalled)
        assertTrue(result is com.mesh.emergency.core.common.result.Result.Error)
    }

    @Test
    fun `runIfAllowed executes block in NORMAL mode`() = runTest {
        val scheduler = makeScheduler(PowerSavingMode.NORMAL)
        var blockCalled = false
        val result = scheduler.runIfAllowed<Unit> {
            blockCalled = true
            com.mesh.emergency.core.common.result.Result.Success(Unit)
        }
        assertTrue(blockCalled)
        assertTrue(result is com.mesh.emergency.core.common.result.Result.Success)
    }
}
