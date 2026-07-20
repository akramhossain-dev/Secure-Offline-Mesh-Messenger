/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.feature.dashboard

import androidx.lifecycle.viewModelScope
import com.mesh.emergency.core.domain.AppState
import com.mesh.emergency.core.domain.AppStateRepository
import com.mesh.emergency.core.presentation.base.BaseUiEffect
import com.mesh.emergency.core.presentation.base.BaseUiEvent
import com.mesh.emergency.core.presentation.base.BaseUiState
import com.mesh.emergency.core.presentation.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── State ─────────────────────────────────────────────────────────────────────
data class HomeUiState(
    val appName: String = "Emergency Connect",
    val isOnline: Boolean = false,
    val batteryLevel: Float = 1.0f,
    val isCharging: Boolean = false,
    val activeSos: Boolean = false,
    val connectedNodeCount: Int = 0,
    val activeTransport: String = "NONE",
    val recentActivity: List<ActivityItem> = emptyList()
) : BaseUiState

data class ActivityItem(
    val id: String,
    val title: String,
    val description: String,
    val timestamp: Long,
    val type: ActivityType
)

enum class ActivityType { MESSAGE, SOS, NODE_JOINED, RESOURCE, SYSTEM }

// ── Events ────────────────────────────────────────────────────────────────────
sealed interface HomeUiEvent : BaseUiEvent {
    data object SosButtonPressed : HomeUiEvent
    data object RefreshStatus    : HomeUiEvent
    data object DismissSos       : HomeUiEvent
}

// ── Effects ───────────────────────────────────────────────────────────────────
sealed interface HomeUiEffect : BaseUiEffect {
    data object NavigateToEmergency : HomeUiEffect
    data class ShowToast(val message: String) : HomeUiEffect
}

// ── ViewModel ─────────────────────────────────────────────────────────────────
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val appStateRepository: AppStateRepository
) : BaseViewModel<HomeUiState, HomeUiEvent, HomeUiEffect>(HomeUiState()) {

    init {
        observeAppState()
    }

    private fun observeAppState() {
        viewModelScope.launch {
            appStateRepository.appState.collect { state ->
                updateState {
                    copy(
                        isOnline          = state.isOnline,
                        batteryLevel      = state.batteryLevel,
                        isCharging        = state.isCharging,
                        activeSos         = state.activeSos,
                        connectedNodeCount = state.connectedNodeCount,
                        activeTransport   = state.activeTransport
                    )
                }
            }
        }
    }

    override fun onEvent(event: HomeUiEvent) {
        when (event) {
            HomeUiEvent.SosButtonPressed -> {
                viewModelScope.launch {
                    appStateRepository.setActiveSos(true)
                }
                sendEffect(HomeUiEffect.NavigateToEmergency)
            }
            HomeUiEvent.RefreshStatus -> {
                sendEffect(HomeUiEffect.ShowToast("Scanning for nearby nodes…"))
            }
            HomeUiEvent.DismissSos -> {
                viewModelScope.launch {
                    appStateRepository.setActiveSos(false)
                }
            }
        }
    }
}
