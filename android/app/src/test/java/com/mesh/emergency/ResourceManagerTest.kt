/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency

import com.mesh.emergency.core.common.result.Result
import com.mesh.emergency.core.communication.CommunicationManager
import com.mesh.emergency.core.communication.DeliveryResult
import com.mesh.emergency.data.local.LocalDataSource
import com.mesh.emergency.data.local.entity.DbResourceStatus
import com.mesh.emergency.data.local.entity.ResourceEntity
import com.mesh.emergency.data.resource.ResourceManagerImpl
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
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations

/**
 * Unit test validating ResourceManager categories matching and expiration sweeps.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ResourceManagerTest {

    @Mock
    private lateinit var mockLocalDataSource: LocalDataSource

    @Mock
    private lateinit var mockCommunicationManager: CommunicationManager

    private lateinit var resourceManager: ResourceManagerImpl

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        resourceManager = ResourceManagerImpl(mockLocalDataSource, mockCommunicationManager)
    }

    @Test
    fun testCreateOffer_savesToDBAndTriggersBroadcast() = runTest {
        `when`(mockCommunicationManager.sendMessage(any())).thenReturn(
            Result.Success(DeliveryResult.SENT)
        )

        val result = resourceManager.createOffer(
            name = "Painkillers",
            type = "MEDICAL",
            quantity = 10,
            latitude = 23.8,
            longitude = 90.4,
            description = "Paracetamol packs",
            privacy = "PUBLIC"
        )

        assertTrue(result is Result.Success)
        val offer = (result as Result.Success).data
        assertEquals("Painkillers", offer.name)
        assertEquals("MEDICAL", offer.type)

        verify(mockLocalDataSource).insertResource(any())
        verify(mockCommunicationManager).sendMessage(any())
    }

    @Test
    fun testMatchRequestToOffer_returnsCorrectMatches() = runTest {
        val request = ResourceEntity(
            entityId = "req_1",
            ownerId = "user_1",
            name = "Need Water",
            type = "FOOD",
            quantity = 5,
            latitude = 0.0,
            longitude = 0.0,
            description = "",
            availabilityStatus = DbResourceStatus.LIMITED
        )

        val matchingOffer = ResourceEntity(
            entityId = "off_1",
            ownerId = "user_2",
            name = "Mineral Water Cases",
            type = "FOOD",
            quantity = 20,
            latitude = 23.8,
            longitude = 90.4,
            description = "",
            availabilityStatus = DbResourceStatus.AVAILABLE
        )

        `when`(mockLocalDataSource.getResources()).thenReturn(
            flowOf(listOf(request, matchingOffer))
        )

        val result = resourceManager.matchRequestToOffer("req_1")
        assertTrue(result is Result.Success)

        val matches = (result as Result.Success).data
        assertEquals(1, matches.size)
        assertEquals("off_1", matches[0].entityId)
    }

    @Test
    fun testExpireResources_updatesExpiredStatuses() = runTest {
        val expiredEntity = ResourceEntity(
            entityId = "off_expired",
            ownerId = "user_1",
            name = "Cooked Rice",
            type = "FOOD",
            quantity = 1,
            latitude = 23.8,
            longitude = 90.4,
            description = "Spoils quickly",
            availabilityStatus = DbResourceStatus.AVAILABLE,
            ttl = System.currentTimeMillis() - 1000 // In the past
        )

        `when`(mockLocalDataSource.getResources()).thenReturn(flowOf(listOf(expiredEntity)))

        val result = resourceManager.expireResources()
        assertTrue(result is Result.Success)

        verify(mockLocalDataSource).insertResource(
            expiredEntity.copy(availabilityStatus = DbResourceStatus.EXPIRED)
        )
    }
}
