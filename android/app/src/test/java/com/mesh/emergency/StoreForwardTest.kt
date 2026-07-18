/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency

import com.mesh.emergency.core.common.result.Result
import com.mesh.emergency.core.communication.CommunicationManager
import com.mesh.emergency.core.communication.DeliveryResult
import com.mesh.emergency.core.communication.queue.MessageQueueManager
import com.mesh.emergency.data.communication.forward.ForwardingEngineImpl
import com.mesh.emergency.data.local.LocalDataSource
import com.mesh.emergency.data.local.entity.DbDeliveryStatus
import com.mesh.emergency.data.local.entity.DbMessagePriority
import com.mesh.emergency.data.local.entity.DbMessageType
import com.mesh.emergency.data.local.entity.MessageEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.any
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations

/**
 * Unit test validating Store & Forward queue processing and backoff rules.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StoreForwardTest {

    @Mock
    private lateinit var mockQueueManager: MessageQueueManager

    @Mock
    private lateinit var mockCommunicationManager: CommunicationManager

    @Mock
    private lateinit var mockLocalDataSource: LocalDataSource

    private lateinit var forwardingEngine: ForwardingEngineImpl

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        forwardingEngine = ForwardingEngineImpl(
            mockQueueManager,
            mockCommunicationManager,
            mockLocalDataSource
        )
    }

    @Test
    fun testProcessQueue_updatesStatusToExpired_whenTTLExceeded() = runTest {
        val expiredMessage = MessageEntity(
            entityId = "msg_1",
            conversationId = "recipient_id",
            senderId = "sender",
            recipientId = "recipient_id",
            content = "expired content",
            timestamp = System.currentTimeMillis() - 10000L,
            deliveryStatus = DbDeliveryStatus.PENDING,
            type = DbMessageType.TEXT,
            priority = DbMessagePriority.MEDIUM,
            expiryTime = System.currentTimeMillis() - 1000L, // Expired 1s ago
            retryCount = 0
        )

        `when`(mockQueueManager.getPendingQueue()).thenReturn(flowOf(listOf(expiredMessage)))

        forwardingEngine.processQueue()

        verify(mockQueueManager).updateStatus("msg_1", DbDeliveryStatus.EXPIRED)
        verify(mockCommunicationManager, never()).sendMessage(any())
    }
}
