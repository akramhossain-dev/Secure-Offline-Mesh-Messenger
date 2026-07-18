/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mesh.emergency.core.common.result.Result
import com.mesh.emergency.core.network.NetworkHealthManager
import com.mesh.emergency.core.network.NodeDiscoveryManager
import com.mesh.emergency.domain.repository.NodeDomainModel
import com.mesh.emergency.domain.repository.NodeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Network Health Dashboard screen.
 *
 * Collects real-time data from [NetworkHealthManager] and [NodeDiscoveryManager]
 * to display mesh connectivity diagnostics.
 */
@HiltViewModel
class NetworkHealthViewModel @Inject constructor(
    private val networkHealthManager: NetworkHealthManager,
    private val nodeDiscoveryManager: NodeDiscoveryManager,
    private val nodeRepository: NodeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NetworkHealthUiState())
    val uiState: StateFlow<NetworkHealthUiState> = _uiState.asStateFlow()

    init {
        observeNetworkHealth()
        observeNodes()
    }

    private fun observeNetworkHealth() {
        viewModelScope.launch {
            combine(
                networkHealthManager.availableNodesCount,
                networkHealthManager.activeConnectionsCount,
                networkHealthManager.networkFailureRate,
                networkHealthManager.averageSignalQuality
            ) { nodes, connections, failureRate, signalQuality ->
                NetworkHealthSnapshot(
                    nodeCount = nodes,
                    connectionCount = connections,
                    failureRate = failureRate,
                    signalQuality = signalQuality
                )
            }.collect { snapshot ->
                _uiState.update { state ->
                    state.copy(
                        connectedNodes = snapshot.nodeCount,
                        activeConnections = snapshot.connectionCount,
                        failureRate = snapshot.failureRate,
                        signalQuality = snapshot.signalQuality,
                        networkStatus = when {
                            snapshot.nodeCount == 0   -> NetworkStatus.NO_CONNECTION
                            snapshot.failureRate > 0.5f -> NetworkStatus.DEGRADED
                            else                      -> NetworkStatus.CONNECTED
                        },
                        lastActivityTime = System.currentTimeMillis()
                    )
                }
            }
        }
    }

    private fun observeNodes() {
        viewModelScope.launch {
            nodeRepository.getNetworkNodes().collect { result ->
                when (result) {
                    is Result.Success -> _uiState.update { state ->
                        state.copy(recentNodes = result.data.sortedByDescending { it.lastSeen }.take(5))
                    }
                    else -> Unit
                }
            }
        }
    }

    fun onSimulateNodeJoin() {
        networkHealthManager.recordSuccess()
        val current = _uiState.value.connectedNodes
        networkHealthManager.updateCounts(
            nodes = current + 1,
            connections = _uiState.value.activeConnections + 1,
            signal = 75f + (0..20).random()
        )
    }

    fun onSimulateNodeLeave() {
        val current = (_uiState.value.connectedNodes - 1).coerceAtLeast(0)
        networkHealthManager.updateCounts(
            nodes = current,
            connections = (_uiState.value.activeConnections - 1).coerceAtLeast(0),
            signal = _uiState.value.signalQuality
        )
    }

    fun onSimulateFailure() {
        networkHealthManager.recordFailure()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// UI state & models
// ─────────────────────────────────────────────────────────────────────────────

data class NetworkHealthUiState(
    val connectedNodes: Int = 0,
    val activeConnections: Int = 0,
    val failureRate: Float = 0f,
    val signalQuality: Float = 0f,
    val networkStatus: NetworkStatus = NetworkStatus.NO_CONNECTION,
    val connectionType: String = "BLE",
    val recentNodes: List<NodeDomainModel> = emptyList(),
    val lastActivityTime: Long = 0L
)

enum class NetworkStatus(val label: String) {
    CONNECTED("Connected"),
    DEGRADED("Degraded"),
    NO_CONNECTION("No Connection")
}

private data class NetworkHealthSnapshot(
    val nodeCount: Int,
    val connectionCount: Int,
    val failureRate: Float,
    val signalQuality: Float
)
