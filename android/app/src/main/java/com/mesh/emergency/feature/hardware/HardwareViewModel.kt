/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.feature.hardware

import androidx.lifecycle.viewModelScope
import com.mesh.emergency.core.hardware.BleConnectionResult
import com.mesh.emergency.core.hardware.BleDevice
import com.mesh.emergency.core.hardware.BleDiscoveryState
import com.mesh.emergency.core.hardware.Esp32Command
import com.mesh.emergency.core.hardware.HardwareCommandResult
import com.mesh.emergency.core.hardware.HardwareDeviceProfile
import com.mesh.emergency.core.hardware.HardwareManager
import com.mesh.emergency.core.presentation.base.BaseUiEffect
import com.mesh.emergency.core.presentation.base.BaseUiEvent
import com.mesh.emergency.core.presentation.base.BaseUiState
import com.mesh.emergency.core.presentation.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── State ─────────────────────────────────────────────────────────────────────
data class HardwareUiState(
    val discoveredDevices: List<BleDevice> = emptyList(),
    val connectedProfile: HardwareDeviceProfile? = null,
    val discoveryState: BleDiscoveryState = BleDiscoveryState.IDLE,
    val isConnecting: Boolean = false,
    val selectedDevice: BleDevice? = null
) : BaseUiState

// ── Events ────────────────────────────────────────────────────────────────────
sealed interface HardwareUiEvent : BaseUiEvent {
    data object StartScan                              : HardwareUiEvent
    data object StopScan                               : HardwareUiEvent
    data class  ConnectDevice(val macAddress: String)  : HardwareUiEvent
    data object DisconnectDevice                       : HardwareUiEvent
    data object RefreshHardwareStatus                  : HardwareUiEvent
    data object SendGetStatusCommand                   : HardwareUiEvent
    data class  SelectDevice(val device: BleDevice)    : HardwareUiEvent
    data object ClearSelection                         : HardwareUiEvent
}

// ── Effects ───────────────────────────────────────────────────────────────────
sealed interface HardwareUiEffect : BaseUiEffect {
    data class ShowMessage(val text: String)    : HardwareUiEffect
    data class ConnectionSuccess(val name: String) : HardwareUiEffect
    data class ConnectionFailed(val reason: String) : HardwareUiEffect
    data object Disconnected                    : HardwareUiEffect
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

/**
 * A30 ViewModel — exposes BLE discovery, connection, and hardware status
 * to the UI without any direct Android BLE API usage.
 */
@HiltViewModel
class HardwareViewModel @Inject constructor(
    private val hardwareManager: HardwareManager
) : BaseViewModel<HardwareUiState, HardwareUiEvent, HardwareUiEffect>(HardwareUiState()) {

    init {
        observeDiscoveredDevices()
        observeDiscoveryState()
        observeConnectedProfile()
    }

    private fun observeDiscoveredDevices() {
        viewModelScope.launch {
            hardwareManager.knownDevices.collect { devices ->
                updateState { copy(discoveredDevices = devices) }
            }
        }
    }

    private fun observeDiscoveryState() {
        viewModelScope.launch {
            hardwareManager.discoveryState.collect { state ->
                updateState { copy(discoveryState = state) }
            }
        }
    }

    private fun observeConnectedProfile() {
        viewModelScope.launch {
            hardwareManager.connectedProfile.collect { profile ->
                updateState { copy(connectedProfile = profile) }
            }
        }
    }

    override fun onEvent(event: HardwareUiEvent) {
        when (event) {
            HardwareUiEvent.StartScan -> {
                viewModelScope.launch {
                    hardwareManager.startDiscovery()
                }
            }

            HardwareUiEvent.StopScan -> {
                viewModelScope.launch {
                    hardwareManager.stopDiscovery()
                }
            }

            is HardwareUiEvent.ConnectDevice -> {
                viewModelScope.launch {
                    updateState { copy(isConnecting = true) }
                    val result = hardwareManager.connectToDevice(event.macAddress)
                    updateState { copy(isConnecting = false) }
                    when (result) {
                        BleConnectionResult.Success -> {
                            val name = currentState.discoveredDevices
                                .firstOrNull { it.macAddress == event.macAddress }?.name ?: "Device"
                            sendEffect(HardwareUiEffect.ConnectionSuccess(name))
                        }
                        is BleConnectionResult.Failure ->
                            sendEffect(HardwareUiEffect.ConnectionFailed(result.reason.name))
                    }
                }
            }

            HardwareUiEvent.DisconnectDevice -> {
                viewModelScope.launch {
                    hardwareManager.disconnectCurrentDevice()
                    sendEffect(HardwareUiEffect.Disconnected)
                }
            }

            HardwareUiEvent.RefreshHardwareStatus -> {
                viewModelScope.launch {
                    hardwareManager.refreshHardwareStatus()
                    sendEffect(HardwareUiEffect.ShowMessage("Hardware status refreshed"))
                }
            }

            HardwareUiEvent.SendGetStatusCommand -> {
                viewModelScope.launch {
                    val result = hardwareManager.sendCommand(Esp32Command.GetStatus)
                    when (result) {
                        is HardwareCommandResult.Success ->
                            sendEffect(HardwareUiEffect.ShowMessage("GET_STATUS sent"))
                        is HardwareCommandResult.Failure ->
                            sendEffect(HardwareUiEffect.ShowMessage("Command failed: ${result.reason}"))
                        HardwareCommandResult.NoDeviceConnected ->
                            sendEffect(HardwareUiEffect.ShowMessage("No device connected"))
                    }
                }
            }

            is HardwareUiEvent.SelectDevice ->
                updateState { copy(selectedDevice = event.device) }

            HardwareUiEvent.ClearSelection ->
                updateState { copy(selectedDevice = null) }
        }
    }
}
