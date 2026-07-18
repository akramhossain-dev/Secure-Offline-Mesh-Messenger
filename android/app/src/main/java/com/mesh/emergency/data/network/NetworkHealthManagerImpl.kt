/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.network

import com.mesh.emergency.core.network.NetworkHealthManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [NetworkHealthManager] tracking active statistics of mesh connectivity.
 */
@Singleton
class NetworkHealthManagerImpl @Inject constructor() : NetworkHealthManager {

    private val _availableNodesCount = MutableStateFlow(0)
    override val availableNodesCount: StateFlow<Int> = _availableNodesCount.asStateFlow()

    private val _activeConnectionsCount = MutableStateFlow(0)
    override val activeConnectionsCount: StateFlow<Int> = _activeConnectionsCount.asStateFlow()

    private val _networkFailureRate = MutableStateFlow(0.0f)
    override val networkFailureRate: StateFlow<Float> = _networkFailureRate.asStateFlow()

    private val _averageSignalQuality = MutableStateFlow(100.0f)
    override val averageSignalQuality: StateFlow<Float> = _averageSignalQuality.asStateFlow()

    private var totalAttempts = 0
    private var failedAttempts = 0

    override fun recordFailure() {
        totalAttempts++
        failedAttempts++
        recalculateFailureRate()
    }

    override fun recordSuccess() {
        totalAttempts++
        recalculateFailureRate()
    }

    override fun updateCounts(nodes: Int, connections: Int, signal: Float) {
        _availableNodesCount.value = nodes
        _activeConnectionsCount.value = connections
        _averageSignalQuality.value = signal
    }

    private fun recalculateFailureRate() {
        if (totalAttempts > 0) {
            _networkFailureRate.value = failedAttempts.toFloat() / totalAttempts.toFloat()
        }
    }
}
