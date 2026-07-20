/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.network

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface contract monitoring mesh link connection status parameters and diagnostics.
 */
interface NetworkHealthManager {
    /** Number of active nodes in the mesh. */
    val availableNodesCount: StateFlow<Int>

    /** Number of active connection links. */
    val activeConnectionsCount: StateFlow<Int>

    /** Connection failure rates (0.0 to 1.0). */
    val networkFailureRate: StateFlow<Float>

    /** Average link quality signal values (0.0 to 100.0). */
    val averageSignalQuality: StateFlow<Float>

    /** Real-time packets sent counter. */
    val packetsSent: StateFlow<Int>

    /** Real-time packets received counter. */
    val packetsReceived: StateFlow<Int>

    /** Real-time failed packets counter. */
    val failedPackets: StateFlow<Int>

    /** Real-time connection uptime in milliseconds. */
    val connectionUptime: Flow<Long>

    /** Local battery level (0-100), or -1 if unavailable. */
    val localBatteryLevel: Int

    /** Logs a connection attempt failure. */
    fun recordFailure()

    /** Logs a connection attempt success. */
    fun recordSuccess()

    /** Modifies current active counters. */
    fun updateCounts(nodes: Int, connections: Int, signal: Float)
}
