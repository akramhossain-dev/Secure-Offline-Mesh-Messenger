/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency

import com.mesh.emergency.core.common.result.Result
import com.mesh.emergency.data.local.DatabaseOptimizer
import com.mesh.emergency.data.map.MapRepositoryImpl
import com.mesh.emergency.core.map.MapTileModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Performance-related unit tests (A33).
 *
 * Tests:
 * - LRU cache eviction at capacity
 * - Pagination offset calculation
 * - Slow query threshold
 * - measureBlock timing utility
 */
class PerformanceTest {

    @Test
    fun `lru tile cache evicts eldest entry at max capacity`() = runTest {
        val repo = MapRepositoryImpl()

        // Fill to max (100 tiles)
        repeat(100) { i ->
            repo.saveMapTile(MapTileModel("base", 10, i, 0, byteArrayOf(1)))
        }

        val beforeAdd = (repo.getOfflineMapData() as Result.Success).data.tileCount
        assertEquals(100, beforeAdd)

        // Adding 101st should evict the first
        repo.saveMapTile(MapTileModel("base", 10, 101, 0, byteArrayOf(2)))
        val afterAdd = (repo.getOfflineMapData() as Result.Success).data.tileCount
        assertEquals(100, afterAdd) // Should still be 100 after LRU eviction
    }

    @Test
    fun `database optimizer page offset calculation is correct`() {
        assertEquals(0,  DatabaseOptimizer.offsetFor(0))
        assertEquals(20, DatabaseOptimizer.offsetFor(1))
        assertEquals(40, DatabaseOptimizer.offsetFor(2))
        assertEquals(60, DatabaseOptimizer.offsetFor(3))
    }

    @Test
    fun `database optimizer total pages calculation is correct`() {
        assertEquals(0, DatabaseOptimizer.totalPages(0))
        assertEquals(1, DatabaseOptimizer.totalPages(1))
        assertEquals(1, DatabaseOptimizer.totalPages(20))
        assertEquals(2, DatabaseOptimizer.totalPages(21))
        assertEquals(5, DatabaseOptimizer.totalPages(100))
    }

    @Test
    fun `database optimizer custom page size works`() {
        assertEquals(0, DatabaseOptimizer.offsetFor(0, 50))
        assertEquals(50, DatabaseOptimizer.offsetFor(1, 50))
        assertEquals(2, DatabaseOptimizer.totalPages(100, 50))
    }

    @Test
    fun `measure block returns correct result`() {
        val result = com.mesh.emergency.core.system.measureBlock("test") { 42 }
        assertEquals(42, result)
    }

    @Test
    fun `sync queue operations are O(1) enqueue and dequeue`() {
        val statusManager = com.mesh.emergency.core.common.OfflineStatusManager()
        val queue = com.mesh.emergency.core.common.SyncQueueManager(statusManager)
        val operations = (1..1000).map {
            com.mesh.emergency.core.common.SyncOperation(
                id = it.toString(),
                type = com.mesh.emergency.core.common.SyncOperationType.MESSAGE,
                payload = byteArrayOf()
            )
        }

        val startEnqueue = System.currentTimeMillis()
        operations.take(500).forEach { queue.enqueue(it) }
        val enqueueMs = System.currentTimeMillis() - startEnqueue
        assertTrue("Enqueue 500 items should be < 100ms but was ${enqueueMs}ms", enqueueMs < 100)

        val startDequeue = System.currentTimeMillis()
        repeat(500) { queue.dequeueNext() }
        val dequeueMs = System.currentTimeMillis() - startDequeue
        assertTrue("Dequeue 500 items should be < 100ms but was ${dequeueMs}ms", dequeueMs < 100)
    }
}
