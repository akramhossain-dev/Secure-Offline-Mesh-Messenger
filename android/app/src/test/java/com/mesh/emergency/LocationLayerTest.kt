/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency

import com.mesh.emergency.core.common.result.Result
import com.mesh.emergency.core.map.MapState
import com.mesh.emergency.core.utils.LocationData
import com.mesh.emergency.core.utils.LocationState
import com.mesh.emergency.data.local.LocalDataSource
import com.mesh.emergency.data.local.entity.LocationEntity
import com.mesh.emergency.data.map.MapProviderImpl
import com.mesh.emergency.data.repository.LocationRepositoryImpl
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
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations

/**
 * Unit test validating coordinates history and offline map state logic.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LocationLayerTest {

    @Mock
    private lateinit var mockLocalDataSource: LocalDataSource

    @Mock
    private lateinit var mockDeviceFingerprintProvider: DeviceFingerprintProvider

    private lateinit var locationRepository: LocationRepositoryImpl
    private lateinit var mapProvider: MapProviderImpl

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        `when`(mockDeviceFingerprintProvider.getDeviceFingerprint()).thenReturn("local_user_id")
        locationRepository = LocationRepositoryImpl(mockLocalDataSource, mockDeviceFingerprintProvider)
        mapProvider = MapProviderImpl()
    }

    @Test
    fun testSaveLocation_insertsEntityCorrectly() = runTest {
        val data = LocationData(
            id = "loc_1",
            latitude = 23.8103,
            longitude = 90.4125,
            altitude = 12.5,
            accuracy = 4.2f,
            timestamp = System.currentTimeMillis(),
            provider = "gps",
            deviceId = "local_device"
        )

        val result = locationRepository.saveLocation(data)
        assertTrue(result is Result.Success)

        verify(mockLocalDataSource).insertLocation(
            LocationEntity(
                entityId = "loc_1",
                userId = "local_user_id",
                latitude = 23.8103,
                longitude = 90.4125,
                altitude = 12.5,
                accuracy = 4.2f,
                timestamp = data.timestamp,
                provider = "gps",
                deviceId = "local_device"
            )
        )
    }

    @Test
    fun testMapProvider_loadsOfflineFiles() {
        assertEquals(MapState.EMPTY, mapProvider.mapState.value)

        val result = mapProvider.loadOfflineMap("/storage/emulated/0/dhaka.mbtiles")
        assertTrue(result is Result.Success)
        assertEquals(MapState.LOADED, mapProvider.mapState.value)
    }
}
