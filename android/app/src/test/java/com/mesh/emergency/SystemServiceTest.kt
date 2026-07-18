/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency

import android.content.Context
import android.content.pm.PackageManager
import com.mesh.emergency.core.utils.capability.DeviceCapabilityManagerImpl
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

/**
 * Unit test validating DeviceCapabilityManager audits mapping packages manager.
 */
class SystemServiceTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockPackageManager: PackageManager

    private lateinit var capabilityManager: DeviceCapabilityManagerImpl

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        `when`(mockContext.packageManager).thenReturn(mockPackageManager)
        capabilityManager = DeviceCapabilityManagerImpl(mockContext)
    }

    @Test
    fun testBluetoothSupported_whenFeaturePresent() {
        `when`(mockPackageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)).thenReturn(true)
        assertTrue(capabilityManager.isBluetoothSupported())
    }

    @Test
    fun testBluetoothNotSupported_whenFeatureMissing() {
        `when`(mockPackageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)).thenReturn(false)
        `when`(mockPackageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)).thenReturn(false)
        assertFalse(capabilityManager.isBluetoothSupported())
    }

    @Test
    fun testLocationSupported_whenFeaturePresent() {
        `when`(mockPackageManager.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS)).thenReturn(true)
        assertTrue(capabilityManager.isLocationSupported())
    }

    @Test
    fun testMicrophoneSupported_whenFeaturePresent() {
        `when`(mockPackageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)).thenReturn(true)
        assertTrue(capabilityManager.isMicrophoneSupported())
    }
}
