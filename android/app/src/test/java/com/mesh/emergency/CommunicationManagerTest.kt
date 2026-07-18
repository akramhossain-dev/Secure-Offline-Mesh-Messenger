/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency

import com.mesh.emergency.core.common.result.Result
import com.mesh.emergency.core.communication.CommunicationState
import com.mesh.emergency.core.communication.TransportStatus
import com.mesh.emergency.data.communication.CommunicationManagerImpl
import com.mesh.emergency.data.communication.MockTransport
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit test validating CommunicationManager priority selectors logic.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CommunicationManagerTest {

    private lateinit var communicationManager: CommunicationManagerImpl
    private lateinit var mockTransport: MockTransport

    @Before
    fun setUp() {
        communicationManager = CommunicationManagerImpl()
        mockTransport = MockTransport()
    }

    @Test
    fun testRegisterTransport_activatesChannels() = runTest {
        mockTransport.setMockStatus(TransportStatus.CONNECTED)
        communicationManager.registerTransport(mockTransport)

        assertEquals(mockTransport, communicationManager.activeTransport.value)
        assertEquals(CommunicationState.CONNECTED, communicationManager.communicationState.value)
    }

    @Test
    fun testSendMessage_failsWhenDisconnected() = runTest {
        mockTransport.setMockStatus(TransportStatus.DISCONNECTED)
        communicationManager.registerTransport(mockTransport)

        val result = communicationManager.sendMessage("test".toByteArray())
        assertTrue(result is Result.Error)
    }

    @Test
    fun testSendMessage_succeedsWhenConnected() = runTest {
        mockTransport.setMockStatus(TransportStatus.CONNECTED)
        communicationManager.registerTransport(mockTransport)

        val result = communicationManager.sendMessage("hello".toByteArray())
        assertTrue(result is Result.Success)
    }

    @Test
    fun testUnregisterTransport_cleansState() = runTest {
        mockTransport.setMockStatus(TransportStatus.CONNECTED)
        communicationManager.registerTransport(mockTransport)
        communicationManager.unregisterTransport(mockTransport.type)

        assertEquals(null, communicationManager.activeTransport.value)
        assertEquals(CommunicationState.UNAVAILABLE, communicationManager.communicationState.value)
    }
}
