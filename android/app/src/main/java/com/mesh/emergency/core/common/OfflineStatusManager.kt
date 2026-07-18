/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.common

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages offline status indicators and sync queue metrics.
 *
 * This app is always offline-first — internet connectivity is never required.
 * "Connectivity" here refers to mesh network availability.
 */
@Singleton
class OfflineStatusManager @Inject constructor() {

    private val _isMeshConnected = MutableStateFlow(false)
    /** True when at least one mesh node is reachable. */
    val isMeshConnected: StateFlow<Boolean> = _isMeshConnected.asStateFlow()

    private val _syncQueueSize = MutableStateFlow(0)
    /** Number of messages/packets queued for delivery when mesh becomes available. */
    val syncQueueSize: StateFlow<Int> = _syncQueueSize.asStateFlow()

    private val _offlineIndicatorVisible = MutableStateFlow(true)
    /** Controls visibility of offline mode indicator UI. */
    val offlineIndicatorVisible: StateFlow<Boolean> = _offlineIndicatorVisible.asStateFlow()

    private val _lastSyncTime = MutableStateFlow(0L)
    /** Timestamp of last successful mesh sync in epoch ms. */
    val lastSyncTime: StateFlow<Long> = _lastSyncTime.asStateFlow()

    /** Update mesh connection state. */
    fun setMeshConnected(connected: Boolean) {
        _isMeshConnected.value = connected
        _offlineIndicatorVisible.value = !connected
        if (connected) {
            _lastSyncTime.value = System.currentTimeMillis()
        }
    }

    /** Increment pending sync queue counter. */
    fun enqueue() {
        _syncQueueSize.value = _syncQueueSize.value + 1
    }

    /** Decrement pending sync queue counter (item delivered). */
    fun dequeue() {
        _syncQueueSize.value = (_syncQueueSize.value - 1).coerceAtLeast(0)
    }

    /** Reset queue counter after bulk delivery. */
    fun resetQueue() {
        _syncQueueSize.value = 0
        _lastSyncTime.value = System.currentTimeMillis()
    }
}
