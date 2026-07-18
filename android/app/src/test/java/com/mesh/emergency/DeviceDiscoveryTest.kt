/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency

import com.mesh.emergency.core.discovery.qr.QRHandshakeData
import com.mesh.emergency.core.discovery.qr.QRHandshakeManager
import com.mesh.emergency.data.local.LocalDataSource
import com.mesh.emergency.data.local.entity.DbTrustStatus
import com.mesh.emergency.data.local.entity.DeviceEntity
import com.mesh.emergency.data.repository.DeviceRepositoryImpl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations

/**
 * Unit test validating QR code handshake conversion and trust persistence.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DeviceDiscoveryTest {

    @Mock
    private lateinit var mockLocalDataSource: LocalDataSource

    private lateinit var deviceRepository: DeviceRepositoryImpl

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        deviceRepository = DeviceRepositoryImpl(mockLocalDataSource)
    }

    @Test
    fun testQRHandshakeSerializationRoundtrip() {
        val original = QRHandshakeData(
            deviceId = "dev_alice_id",
            userId = "usr_alice_id",
            deviceType = "SMARTPHONE",
            publicKeyRef = "alice_pub_key_base64",
            timestamp = System.currentTimeMillis()
        )

        val payload = QRHandshakeManager.generatePayload(original)
        val parsed = QRHandshakeManager.parsePayload(payload)

        assertEquals(original.deviceId, parsed.deviceId)
        assertEquals(original.userId, parsed.userId)
        assertEquals(original.publicKeyRef, parsed.publicKeyRef)
    }

    @Test
    fun testUpdateTrustStatus_savesUpdatedEntity() = runTest {
        val device = DeviceEntity(
            entityId = "dev_1",
            name = "Test Node",
            rssi = -80,
            lastSeen = System.currentTimeMillis(),
            trustStatus = DbTrustStatus.UNKNOWN
        )

        `when`(mockLocalDataSource.getDeviceById("dev_1")).thenReturn(device)

        val result = deviceRepository.updateTrustStatus("dev_1", "TRUSTED")

        assertTrue(result is com.mesh.emergency.core.common.result.Result.Success)
        verify(mockLocalDataSource).insertDevice(
            device.copy(trustStatus = DbTrustStatus.TRUSTED)
        )
    }
}
