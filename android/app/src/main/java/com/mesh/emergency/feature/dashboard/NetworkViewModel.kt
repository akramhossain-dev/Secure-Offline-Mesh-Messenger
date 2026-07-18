/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mesh.emergency.core.common.config.FeatureFlags
import com.mesh.emergency.core.communication.CommunicationManager
import com.mesh.emergency.core.communication.CommunicationState
import com.mesh.emergency.core.communication.TransportType
import com.mesh.emergency.core.network.NetworkHealthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NetworkUiState(
    val isConnected: Boolean = false,
    val nodeCount: Int = 0,
    val transport: String = "NONE",
    val rssi: Int = 0,
    val isBluetoothConnected: Boolean = false,
    val isBluetoothEnabled: Boolean = false,
    val isLoRaConnected: Boolean = false,
    val isLoRaEnabled: Boolean = false
)

@HiltViewModel
class NetworkViewModel @Inject constructor(
    private val communicationManager: CommunicationManager,
    private val networkHealthManager: NetworkHealthManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(NetworkUiState())
    val uiState: StateFlow<NetworkUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                communicationManager.communicationState,
                communicationManager.activeTransport,
                networkHealthManager.availableNodesCount,
                networkHealthManager.averageSignalQuality
            ) { commState, activeTransport, nodes, signal ->
                val connected = commState == CommunicationState.CONNECTED ||
                        commState == CommunicationState.SENDING ||
                        commState == CommunicationState.RECEIVING
                val transportName = activeTransport?.type?.name ?: "NONE"
                // Map signal quality (0.0 - 100.0) to a rough RSSI value (-100 to -30 dBm)
                val rssiDbm = if (connected) {
                    (-100 + (signal * 0.7f)).toInt().coerceIn(-100, -30)
                } else {
                    -100
                }
                val isBleConnected = connected && activeTransport?.type == TransportType.BLUETOOTH
                val isLoraConnected = connected && activeTransport?.type == TransportType.LORA
                NetworkUiState(
                    isConnected = connected,
                    nodeCount = nodes,
                    transport = transportName,
                    rssi = rssiDbm,
                    isBluetoothConnected = isBleConnected,
                    isBluetoothEnabled = FeatureFlags.FEATURE_BLE,
                    isLoRaConnected = isLoraConnected,
                    isLoRaEnabled = FeatureFlags.FEATURE_LORA
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }
}
