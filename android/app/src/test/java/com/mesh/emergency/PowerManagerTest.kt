/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency

import com.mesh.emergency.core.power.PowerEvent
import com.mesh.emergency.core.power.PowerSavingMode
import com.mesh.emergency.data.power.PowerManagerImpl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit test validating PowerManager battery tracking and optimization profiles.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PowerManagerTest {

    private lateinit var powerManager: PowerManagerImpl

    @Before
    fun setUp() {
        powerManager = PowerManagerImpl()
    }

    @Test
    fun testBatteryThresholds_triggerSavingAndEmergencyProfiles() = runTest {
        // Normal battery levels
        powerManager.handleBatteryUpdate(80, false)
        assertEquals(PowerSavingMode.NORMAL, powerManager.currentMode.value)

        // Low Battery Threshold (<= 20%)
        powerManager.handleBatteryUpdate(18, false)
        assertEquals(PowerSavingMode.SAVING, powerManager.currentMode.value)

        // Critical Battery Threshold (<= 10%)
        powerManager.handleBatteryUpdate(8, false)
        assertEquals(PowerSavingMode.EMERGENCY, powerManager.currentMode.value)
    }

    @Test
    fun testChargingState_resetsProfileToNormal() = runTest {
        powerManager.handleBatteryUpdate(8, false)
        assertEquals(PowerSavingMode.EMERGENCY, powerManager.currentMode.value)

        // Plugged in
        powerManager.handleBatteryUpdate(8, true)
        assertEquals(PowerSavingMode.NORMAL, powerManager.currentMode.value)
    }

    @Test
    fun testPowerEvents_emitLowAndCriticalWarnings() = runTest {
        val events = mutableListOf<PowerEvent>()
        val collectJob = launch(UnconfinedTestDispatcher()) {
            powerManager.powerEvents.toList(events)
        }

        // Low Battery (18) and Plugged in (true)
        powerManager.handleBatteryUpdate(18, true)
        powerManager.handleBatteryUpdate(18, false) // Unplugged
        powerManager.handleBatteryUpdate(8, false)  // Critical

        assertEquals(3, events.size)
        assertTrue(events[0] is PowerEvent.ChargingStarted)
        assertTrue(events[1] is PowerEvent.ChargingStopped)
        assertTrue(events[2] is PowerEvent.PowerCritical)

        collectJob.cancel()
    }
}
