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
import com.mesh.emergency.data.local.LocalDataSource
import com.mesh.emergency.domain.repository.NodeDomainModel
import com.mesh.emergency.domain.repository.NodeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
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
    private val nodeRepository: NodeRepository,
    private val localDataSource: LocalDataSource
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
                networkHealthManager.averageSignalQuality,
                networkHealthManager.packetsSent,
                networkHealthManager.packetsReceived,
                networkHealthManager.failedPackets,
                networkHealthManager.connectionUptime
            ) { args ->
                val nodes = args[0] as Int
                val connections = args[1] as Int
                val failureRate = args[2] as Float
                val signalQuality = args[3] as Float
                val sent = args[4] as Int
                val received = args[5] as Int
                val failed = args[6] as Int
                val uptimeMs = args[7] as Long

                val uptimeLabel = if (uptimeMs <= 0L) {
                    "Unavailable"
                } else {
                    val sec = (uptimeMs / 1000) % 60
                    val min = (uptimeMs / (1000 * 60)) % 60
                    val hr = (uptimeMs / (1000 * 60 * 60))
                    if (hr > 0) "${hr}h ${min}m ${sec}s"
                    else if (min > 0) "${min}m ${sec}s"
                    else "${sec}s"
                }

                val batteryVal = networkHealthManager.localBatteryLevel
                val batteryLabel = if (batteryVal in 0..100) "$batteryVal%" else "Unavailable"

                // Determine connection topology based on actual links
                val topologyLabel = when {
                    connections == 0 -> "Offline"
                    connections == 1 -> "Point-to-Point (P2P)"
                    connections > 1  -> "Star Topology (Hub)"
                    else             -> "Unavailable"
                }

                NetworkHealthTelemetrySnapshot(
                    nodeCount = nodes,
                    connectionCount = connections,
                    failureRate = failureRate,
                    signalQuality = signalQuality,
                    packetsSent = sent,
                    packetsReceived = received,
                    failedPackets = failed,
                    uptimeLabel = uptimeLabel,
                    batteryLabel = batteryLabel,
                    topologyLabel = topologyLabel
                )
            }.collect { snapshot ->
                _uiState.update { state ->
                    state.copy(
                        connectedNodes = snapshot.nodeCount,
                        activeConnections = snapshot.connectionCount,
                        failureRate = snapshot.failureRate,
                        signalQuality = snapshot.signalQuality,
                        packetsSentCount = snapshot.packetsSent,
                        packetsReceivedCount = snapshot.packetsReceived,
                        failedPacketsCount = snapshot.failedPackets,
                        uptimeLabel = snapshot.uptimeLabel,
                        batteryLevelLabel = snapshot.batteryLabel,
                        topologyLabel = snapshot.topologyLabel,
                        networkStatus = when {
                            snapshot.connectionCount == 0 -> NetworkStatus.NO_CONNECTION
                            snapshot.failureRate > 0.5f   -> NetworkStatus.DEGRADED
                            else                          -> NetworkStatus.CONNECTED
                        },
                        lastActivityTime = System.currentTimeMillis()
                    )
                }
            }
        }
    }

    private fun observeNodes() {
        viewModelScope.launch {
            combine(
                nodeRepository.getNetworkNodes(),
                localDataSource.getDevices()
            ) { nodesResult, devices ->
                val activeNodes = (nodesResult as? Result.Success)?.data ?: emptyList()
                val validDevices = devices.filter { !it.entityId.contains(":") }
                
                val localUser = try { localDataSource.getCurrentUser().firstOrNull() } catch (e: Exception) { null }
                val localBattery = networkHealthManager.localBatteryLevel
                
                val uiNodes = mutableListOf<NetworkHealthNodeUiModel>()
                
                if (localUser != null) {
                    uiNodes.add(
                        NetworkHealthNodeUiModel(
                            id = localUser.entityId,
                            deviceName = "${localUser.username} (You)",
                            type = "PHONE_NODE",
                            status = "ONLINE",
                            rssi = 0,
                            lastSeen = System.currentTimeMillis(),
                            batteryLevel = if (localBattery in 0..100) localBattery else 100,
                            messageCount = 0
                        )
                    )
                }
                
                validDevices.forEach { device ->
                    val activeNode = activeNodes.firstOrNull { it.id == device.entityId }
                    val count = try {
                        localDataSource.getMessagesForConversation(device.entityId).firstOrNull()?.size ?: 0
                    } catch (e: Exception) {
                        0
                    }
                    uiNodes.add(
                        NetworkHealthNodeUiModel(
                            id = device.entityId,
                            deviceName = device.nickname?.takeIf { it.isNotBlank() } ?: device.name,
                            type = activeNode?.type ?: device.deviceType,
                            status = activeNode?.status ?: "OFFLINE",
                            rssi = activeNode?.rssi ?: -100,
                            lastSeen = activeNode?.lastSeen ?: device.lastSeen,
                            batteryLevel = activeNode?.batteryLevel ?: -1,
                            messageCount = count
                        )
                    )
                }
                uiNodes
            }.collect { uiNodes ->
                _uiState.update { state ->
                    state.copy(
                        recentNodes = uiNodes.sortedWith(
                            compareByDescending<NetworkHealthNodeUiModel> { it.status == "ONLINE" }
                                .thenByDescending { it.lastSeen }
                        ).take(10)
                    )
                }
            }
        }
    }

    fun onSimulateNodeJoin() {
        // No-op: Simulation disabled for real hardware telemetry
    }

    fun onSimulateNodeLeave() {
        // No-op: Simulation disabled for real hardware telemetry
    }

    fun onSimulateFailure() {
        // No-op: Simulation disabled for real hardware telemetry
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
    val recentNodes: List<NetworkHealthNodeUiModel> = emptyList(),
    val lastActivityTime: Long = 0L,
    val uptimeLabel: String = "Unavailable",
    val packetsSentCount: Int = 0,
    val packetsReceivedCount: Int = 0,
    val failedPacketsCount: Int = 0,
    val batteryLevelLabel: String = "Unavailable",
    val topologyLabel: String = "Unavailable"
)

data class NetworkHealthNodeUiModel(
    val id: String,
    val deviceName: String,
    val type: String,
    val status: String,
    val rssi: Int,
    val lastSeen: Long,
    val batteryLevel: Int,
    val messageCount: Int
)

enum class NetworkStatus(val label: String) {
    CONNECTED("Connected"),
    DEGRADED("Degraded"),
    NO_CONNECTION("No Connection")
}

private data class NetworkHealthTelemetrySnapshot(
    val nodeCount: Int,
    val connectionCount: Int,
    val failureRate: Float,
    val signalQuality: Float,
    val packetsSent: Int,
    val packetsReceived: Int,
    val failedPackets: Int,
    val uptimeLabel: String,
    val batteryLabel: String,
    val topologyLabel: String
)
