/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency

import com.mesh.emergency.core.common.result.Result
import com.mesh.emergency.core.communication.CommunicationManager
import com.mesh.emergency.core.communication.DeliveryResult
import com.mesh.emergency.data.emergency.EmergencyManagerImpl
import com.mesh.emergency.data.local.LocalDataSource
import com.mesh.emergency.data.local.entity.DbEmergencyStatus
import com.mesh.emergency.data.local.entity.EmergencyEventEntity
import com.mesh.emergency.core.identity.DeviceFingerprintProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.any
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations

/**
 * Unit test validating EmergencyManager SOS trigger pipelines and lifecycle status changes.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EmergencyManagerTest {

    @Mock
    private lateinit var mockLocalDataSource: LocalDataSource

    @Mock
    private lateinit var mockCommunicationManager: CommunicationManager

    @Mock
    private lateinit var mockDeviceFingerprintProvider: DeviceFingerprintProvider

    private lateinit var emergencyManager: EmergencyManagerImpl

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        `when`(mockDeviceFingerprintProvider.getDeviceFingerprint()).thenReturn("local_user_id")
        emergencyManager = EmergencyManagerImpl(
            mockLocalDataSource,
            mockCommunicationManager,
            mockDeviceFingerprintProvider
        )
    }

    @Test
    fun testTriggerSOS_forwardPayloadAndSavesEvents() = runTest {
        `when`(mockCommunicationManager.sendMessage(anyByteArray())).thenReturn(
            Result.Success(DeliveryResult.SENT)
        )

        val result = emergencyManager.triggerSOS(23.8103, 90.4125, "SOS medical help needed")
        assertTrue(result is Result.Success)

        val event = (result as Result.Success).data
        assertEquals("local_user_id", event.senderId)
        assertEquals(23.8103, event.latitude, 0.0001)
        assertEquals(DbEmergencyStatus.BROADCASTING, event.status)

        verify(mockCommunicationManager).sendMessage(anyByteArray())
        verify(mockLocalDataSource, times(3)).insertEmergencyEvent(anyEmergencyEventEntity())
    }

    @Test
    fun testResolveEmergency_updatesStatusToResolved() = runTest {
        val event = EmergencyEventEntity(
            entityId = "sos_id_1",
            senderId = "local_user_id",
            latitude = 23.8,
            longitude = 90.4,
            message = "accident situational help",
            timestamp = System.currentTimeMillis(),
            isResolved = false,
            status = DbEmergencyStatus.BROADCASTING
        )

        `when`(mockLocalDataSource.getEmergencyEvents()).thenReturn(flowOf(listOf(event)))

        val result = emergencyManager.resolveEmergency("sos_id_1")
        assertTrue(result is Result.Success)

        verify(mockLocalDataSource).insertEmergencyEvent(
            event.copy(status = DbEmergencyStatus.RESOLVED, isResolved = true)
        )
    }
}

private fun anyByteArray(): ByteArray {
    org.mockito.Mockito.any(ByteArray::class.java)
    return ByteArray(0)
}

private fun anyEmergencyEventEntity(): EmergencyEventEntity {
    org.mockito.Mockito.any(EmergencyEventEntity::class.java)
    return EmergencyEventEntity(
        entityId = "",
        senderId = "",
        latitude = 0.0,
        longitude = 0.0,
        message = "",
        timestamp = 0L,
        isResolved = false
    )
}
