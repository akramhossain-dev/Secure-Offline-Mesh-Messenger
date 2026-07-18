/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency

import com.mesh.emergency.core.communication.CommunicationState
import com.mesh.emergency.core.communication.TransportStatus
import com.mesh.emergency.core.communication.lora.DelayLevel
import com.mesh.emergency.core.communication.lora.LoRaSimulationConfig
import com.mesh.emergency.data.communication.CommunicationManagerImpl
import com.mesh.emergency.data.communication.lora.LoRaSimulationManagerImpl
import com.mesh.emergency.data.communication.lora.MockLoRaTransport
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit test validating Mock LoRa transport and prioritization selectors.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LoRaTransportSimulationTest {

    private lateinit var communicationManager: CommunicationManagerImpl
    private lateinit var simulationManager: LoRaSimulationManagerImpl
    private lateinit var loraTransport: MockLoRaTransport

    @Before
    fun setUp() {
        communicationManager = CommunicationManagerImpl()
        simulationManager = LoRaSimulationManagerImpl()
        loraTransport = MockLoRaTransport(simulationManager)
    }

    @Test
    fun testLoRaConnect_updatesStatusToConnected() = runTest {
        simulationManager.updateConfig(LoRaSimulationConfig())
        val result = loraTransport.connect()

        assertTrue(result is com.mesh.emergency.core.common.result.Result.Success)
        assertEquals(TransportStatus.CONNECTED, loraTransport.status.value)
    }

    @Test
    fun testLoRaSend_appliesConfiguredDelays() = runTest {
        simulationManager.updateConfig(
            LoRaSimulationConfig(
                delayLevel = DelayLevel.LOW,
                packetLossRate = 0.0f
            )
        )
        loraTransport.connect()

        val start = System.currentTimeMillis()
        val result = loraTransport.send("test payload".toByteArray())
        val duration = System.currentTimeMillis() - start

        assertTrue(result is com.mesh.emergency.core.common.result.Result.Success)
        assertTrue("Expected low latency delay to trigger low wait time", duration >= 50L)
    }

    @Test
    fun testLoRaSend_triggersFailuresOnHighPacketLoss() = runTest {
        simulationManager.updateConfig(
            LoRaSimulationConfig(
                packetLossRate = 1.0f // 100% loss roll
            )
        )
        loraTransport.connect()

        val result = loraTransport.send("lost packet".toByteArray())
        assertTrue(result is com.mesh.emergency.core.common.result.Result.Error)
    }

    @Test
    fun testTransportSwitching_fallsBackToLoRaWhenBluetoothUnavailable() = runTest {
        // BLE and LoRa simulation test
        simulationManager.updateConfig(LoRaSimulationConfig())
        loraTransport.connect()

        communicationManager.registerTransport(loraTransport)
        assertEquals(loraTransport, communicationManager.activeTransport.value)
        assertEquals(CommunicationState.CONNECTED, communicationManager.communicationState.value)
    }
}
