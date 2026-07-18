/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency

import android.content.Context
import com.mesh.emergency.core.communication.TransportStatus
import com.mesh.emergency.core.utils.PermissionManager
import com.mesh.emergency.core.utils.permission.PermissionType
import com.mesh.emergency.data.communication.bluetooth.BluetoothTransportImpl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

/**
 * Unit test validating BLE Transport scan settings boundaries.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BluetoothTransportTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockPermissionManager: PermissionManager

    private lateinit var transport: BluetoothTransportImpl

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        transport = BluetoothTransportImpl(mockContext, mockPermissionManager)
    }

    @Test
    fun testConnect_returnsErrorWhenPermissionsDenied() = runTest {
        `when`(mockPermissionManager.hasPermission(mockContext, PermissionType.BLUETOOTH)).thenReturn(false)

        val result = transport.connect()
        assertTrue(result is com.mesh.emergency.core.common.result.Result.Error)
        assertEquals(TransportStatus.DISCONNECTED, transport.status.value)
    }

    @Test
    fun testDisconnect_alwaysCleansState() = runTest {
        val result = transport.disconnect()
        assertTrue(result is com.mesh.emergency.core.common.result.Result.Success)
        assertEquals(TransportStatus.DISCONNECTED, transport.status.value)
    }
}
