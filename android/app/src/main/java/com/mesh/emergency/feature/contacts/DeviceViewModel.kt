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
import com.mesh.emergency.data.local.LocalDataSource
import com.mesh.emergency.data.local.entity.DbTrustStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
    data class OpenChat(val deviceId: String, val deviceName: String) : DeviceUiEvent
}

// ── Effects ───────────────────────────────────────────────────────────────────
sealed interface DeviceUiEffect : BaseUiEffect {
    data class ShowToast(val message: String) : DeviceUiEffect
    data object NavigateToQrPair : DeviceUiEffect
    data class NavigateToChat(val deviceId: String, val deviceName: String) : DeviceUiEffect
}

// ── ViewModel ─────────────────────────────────────────────────────────────────
@HiltViewModel
class DeviceViewModel @Inject constructor(
    private val localDataSource: LocalDataSource
) : BaseViewModel<DeviceUiState, DeviceUiEvent, DeviceUiEffect>(DeviceUiState()) {

    init {
        observeDevices()
    }

    private fun observeDevices() {
        viewModelScope.launch {
            localDataSource.getDevices().collect { devices ->
                updateState {
                    copy(
                        pairedDevices = devices.map { entity ->
                            val timeLabel = try {
                                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(entity.lastSeen))
                            } catch (e: Exception) {
                                "unknown"
                            }
                            DeviceDisplayModel(
                                id = entity.entityId,
                                name = entity.nickname ?: entity.name,
                                macAddress = entity.entityId,
                                isTrusted = entity.trustStatus == DbTrustStatus.TRUSTED,
                                lastSeenLabel = "Seen at: $timeLabel",
                                transport = entity.deviceType,
                                rssi = entity.rssi
                            )
                        }
                    )
                }
            }
        }
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
                viewModelScope.launch {
                    val device = localDataSource.getDeviceById(event.deviceId)
                    if (device != null) {
                        localDataSource.deleteDevice(device)
                        sendEffect(DeviceUiEffect.ShowToast("Device removed"))
                    }
                }
            }
            is DeviceUiEvent.OpenChat -> {
                sendEffect(DeviceUiEffect.NavigateToChat(event.deviceId, event.deviceName))
            }
        }
    }
}
