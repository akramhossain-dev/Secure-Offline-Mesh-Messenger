/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.feature.contacts

import androidx.lifecycle.viewModelScope
import com.mesh.emergency.core.presentation.base.BaseUiEffect
import com.mesh.emergency.core.presentation.base.BaseUiEvent
import com.mesh.emergency.core.presentation.base.BaseUiState
import com.mesh.emergency.core.presentation.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── State ─────────────────────────────────────────────────────────────────────
data class DeviceUiState(
    val pairedDevices: List<DeviceDisplayModel> = emptyList(),
    val isScanning: Boolean = false
) : BaseUiState

data class DeviceDisplayModel(
    val id: String,
    val name: String,
    val macAddress: String,
    val isTrusted: Boolean,
    val lastSeenLabel: String,
    val transport: String,
    val rssi: Int
)

// ── Events ────────────────────────────────────────────────────────────────────
sealed interface DeviceUiEvent : BaseUiEvent {
    data object StartScan : DeviceUiEvent
    data class UnpairDevice(val deviceId: String) : DeviceUiEvent
}

// ── Effects ───────────────────────────────────────────────────────────────────
sealed interface DeviceUiEffect : BaseUiEffect {
    data class ShowToast(val message: String) : DeviceUiEffect
    data object NavigateToQrPair : DeviceUiEffect
}

// ── ViewModel ─────────────────────────────────────────────────────────────────
@HiltViewModel
class DeviceViewModel @Inject constructor() :
    BaseViewModel<DeviceUiState, DeviceUiEvent, DeviceUiEffect>(DeviceUiState()) {

    init { loadDemoDevices() }

    private fun loadDemoDevices() {
        val demo = listOf(
            DeviceDisplayModel("d1", "Node Alpha", "AA:BB:CC:DD:EE:FF", true, "2 min ago", "BLUETOOTH", -62),
            DeviceDisplayModel("d2", "Node Beta",  "11:22:33:44:55:66", true, "15 min ago","BLUETOOTH", -78),
            DeviceDisplayModel("d3", "Field Unit 03", "DE:AD:BE:EF:00:01", false, "1 hr ago","LoRa", -90),
        )
        updateState { copy(pairedDevices = demo) }
    }

    override fun onEvent(event: DeviceUiEvent) {
        when (event) {
            DeviceUiEvent.StartScan -> {
                viewModelScope.launch {
                    updateState { copy(isScanning = true) }
                    kotlinx.coroutines.delay(2000L)
                    updateState { copy(isScanning = false) }
                    sendEffect(DeviceUiEffect.ShowToast("Scan complete — no new nodes found"))
                }
            }
            is DeviceUiEvent.UnpairDevice -> {
                updateState { copy(pairedDevices = pairedDevices.filter { it.id != event.deviceId }) }
                sendEffect(DeviceUiEffect.ShowToast("Device removed"))
            }
        }
    }
}
