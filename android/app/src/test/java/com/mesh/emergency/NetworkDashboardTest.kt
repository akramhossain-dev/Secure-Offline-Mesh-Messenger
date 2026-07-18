/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency

import com.mesh.emergency.core.common.OfflineStatusManager
import com.mesh.emergency.core.common.SyncOperation
import com.mesh.emergency.core.common.SyncOperationType
import com.mesh.emergency.core.common.SyncQueueManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

/**
 * Unit tests for SyncQueueManager and OfflineStatusManager (A32.6).
 *
 * Tests:
 * - Enqueue / dequeue cycle
 * - Queue capacity limit
 * - Offline status indicators
 * - Queue clear
 */
class NetworkDashboardTest {

    private lateinit var offlineStatusManager: OfflineStatusManager
    private lateinit var syncQueueManager: SyncQueueManager

    @Before
    fun setup() {
        offlineStatusManager = OfflineStatusManager()
        syncQueueManager = SyncQueueManager(offlineStatusManager)
    }

    private fun makeOp(type: SyncOperationType = SyncOperationType.MESSAGE) = SyncOperation(
        id = UUID.randomUUID().toString(),
        type = type,
        payload = "test".toByteArray()
    )

    @Test
    fun `enqueue increases queue size`() {
        syncQueueManager.enqueue(makeOp())
        syncQueueManager.enqueue(makeOp())
        assertEquals(2, syncQueueManager.queueSize.value)
    }

    @Test
    fun `dequeue returns operations in FIFO order`() {
        val op1 = makeOp(SyncOperationType.MESSAGE)
        val op2 = makeOp(SyncOperationType.LOCATION)
        syncQueueManager.enqueue(op1)
        syncQueueManager.enqueue(op2)

        val first = syncQueueManager.dequeueNext()
        val second = syncQueueManager.dequeueNext()

        assertEquals(op1.id, first?.id)
        assertEquals(op2.id, second?.id)
    }

    @Test
    fun `dequeue on empty queue returns null`() {
        val result = syncQueueManager.dequeueNext()
        assertNull(result)
    }

    @Test
    fun `clearAll empties queue`() {
        repeat(5) { syncQueueManager.enqueue(makeOp()) }
        syncQueueManager.clearAll()
        assertTrue(syncQueueManager.isEmpty())
        assertEquals(0, syncQueueManager.queueSize.value)
    }

    @Test
    fun `queue rejects operations when at max capacity`() {
        // Fill to capacity
        repeat(SyncQueueManager.MAX_QUEUE_SIZE) { syncQueueManager.enqueue(makeOp()) }
        val result = syncQueueManager.enqueue(makeOp())
        assertFalse(result)
    }

    @Test
    fun `offline status manager mesh connected toggles indicator`() {
        offlineStatusManager.setMeshConnected(true)
        assertFalse(offlineStatusManager.offlineIndicatorVisible.value)
        assertTrue(offlineStatusManager.isMeshConnected.value)

        offlineStatusManager.setMeshConnected(false)
        assertTrue(offlineStatusManager.offlineIndicatorVisible.value)
        assertFalse(offlineStatusManager.isMeshConnected.value)
    }

    @Test
    fun `offline status manager records last sync time on connect`() {
        val before = System.currentTimeMillis()
        offlineStatusManager.setMeshConnected(true)
        val after = System.currentTimeMillis()
        assertTrue(offlineStatusManager.lastSyncTime.value in before..after)
    }
}
