/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency

import com.mesh.emergency.core.common.result.Result
import com.mesh.emergency.core.network.NetworkEvent
import com.mesh.emergency.data.local.LocalDataSource
import com.mesh.emergency.data.local.entity.DbNodeStatus
import com.mesh.emergency.data.local.entity.DbNodeType
import com.mesh.emergency.data.local.entity.NetworkNodeEntity
import com.mesh.emergency.data.network.NetworkHealthManagerImpl
import com.mesh.emergency.data.network.NodeDiscoveryManagerImpl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations

/**
 * Unit test validating NodeDiscoveryManager and NetworkHealthManager.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NetworkAwarenessTest {

    @Mock
    private lateinit var mockLocalDataSource: LocalDataSource

    private lateinit var discoveryManager: NodeDiscoveryManagerImpl
    private lateinit var healthManager: NetworkHealthManagerImpl

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        discoveryManager = NodeDiscoveryManagerImpl(mockLocalDataSource)
        healthManager = NetworkHealthManagerImpl()
    }

    @Test
    fun testDiscoverNode_insertsEntityAndEmitsDiscoveredEvents() = runTest {
        val node = NetworkNodeEntity(
            entityId = "nod_relay_1",
            deviceId = "dev_relay_1",
            nodeType = DbNodeType.RELAY_NODE,
            status = DbNodeStatus.ONLINE,
            rssi = -72
        )

        val events = mutableListOf<NetworkEvent>()
        val collectJob = launch(UnconfinedTestDispatcher()) {
            discoveryManager.networkEvents.toList(events)
        }

        val result = discoveryManager.discoverNode(node)
        assertTrue(result is Result.Success)

        verify(mockLocalDataSource).insertNode(node)

        // Event size should be 2: NodeDiscovered and SignalChanged
        assertEquals(2, events.size)
        assertTrue(events[0] is NetworkEvent.NodeDiscovered)
        assertEquals("nod_relay_1", (events[0] as NetworkEvent.NodeDiscovered).nodeId)

        collectJob.cancel()
    }

    @Test
    fun testNetworkHealthManager_calculatesFailureRate() {
        healthManager.recordSuccess()
        healthManager.recordFailure()
        healthManager.recordSuccess()
        healthManager.recordSuccess()

        // 1 failure out of 4 attempts -> 25% failure rate
        assertEquals(0.25f, healthManager.networkFailureRate.value, 0.001f)
    }
}
