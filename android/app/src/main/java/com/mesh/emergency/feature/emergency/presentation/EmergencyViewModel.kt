/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.feature.emergency.presentation

import androidx.lifecycle.viewModelScope
import com.mesh.emergency.core.presentation.base.BaseUiEffect
import com.mesh.emergency.core.presentation.base.BaseUiEvent
import com.mesh.emergency.core.presentation.base.BaseUiState
import com.mesh.emergency.core.presentation.base.BaseViewModel
import com.mesh.emergency.data.local.entity.DbEmergencyStatus
import com.mesh.emergency.data.local.entity.DbEmergencyType
import com.mesh.emergency.data.local.entity.DbMessagePriority
import com.mesh.emergency.feature.emergency.domain.EmergencyEvent
import com.mesh.emergency.feature.emergency.domain.EmergencyRepository
import com.mesh.emergency.feature.emergency.domain.SosState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

// ── State ─────────────────────────────────────────────────────────────────────
data class EmergencyUiState(
    val sosState: SosState = SosState.READY,
    val activeEvents: List<EmergencyEvent> = emptyList(),
    val historyEvents: List<EmergencyEvent> = emptyList(),
    val isLoading: Boolean = false,
    val selectedEvent: EmergencyEvent? = null
) : BaseUiState

// ── Events ────────────────────────────────────────────────────────────────────
sealed interface EmergencyUiEvent : BaseUiEvent {
    data object InitiateSos          : EmergencyUiEvent
    data object ConfirmSos           : EmergencyUiEvent
    data object CancelSos            : EmergencyUiEvent
    data class ResolveEvent(val id: String)      : EmergencyUiEvent
    data class AcknowledgeEvent(val id: String)  : EmergencyUiEvent
    data class SelectEvent(val event: EmergencyEvent) : EmergencyUiEvent
    data object ClearSelection       : EmergencyUiEvent
}

// ── Effects ───────────────────────────────────────────────────────────────────
sealed interface EmergencyUiEffect : BaseUiEffect {
    data object SosConfirmationRequired  : EmergencyUiEffect
    data object SosActivated             : EmergencyUiEffect
    data object SosCancelled             : EmergencyUiEffect
    data class ShowMessage(val text: String) : EmergencyUiEffect
}

// ── ViewModel ─────────────────────────────────────────────────────────────────
@HiltViewModel
class EmergencyViewModel @Inject constructor(
    private val emergencyRepository: EmergencyRepository
) : BaseViewModel<EmergencyUiState, EmergencyUiEvent, EmergencyUiEffect>(EmergencyUiState()) {

    init {
        observeEvents()
    }

    private fun observeEvents() {
        viewModelScope.launch {
            updateState { copy(isLoading = true) }
            emergencyRepository.getEmergencyEvents().collect { events ->
                val active  = events.filter { !it.isResolved }
                val history = events.filter { it.isResolved }
                updateState {
                    copy(
                        activeEvents  = active,
                        historyEvents = history,
                        isLoading     = false
                    )
                }
            }
        }
    }

    override fun onEvent(event: EmergencyUiEvent) {
        when (event) {
            EmergencyUiEvent.InitiateSos -> {
                updateState { copy(sosState = SosState.CONFIRMING) }
                sendEffect(EmergencyUiEffect.SosConfirmationRequired)
            }

            EmergencyUiEvent.ConfirmSos -> {
                viewModelScope.launch {
                    updateState { copy(sosState = SosState.ACTIVE) }
                    val sosEvent = EmergencyEvent(
                        id        = UUID.randomUUID().toString(),
                        type      = DbEmergencyType.SOS,
                        priority  = DbMessagePriority.CRITICAL,
                        status    = DbEmergencyStatus.BROADCASTING,
                        senderId  = "self",
                        message   = "SOS — Emergency Distress Signal",
                        latitude  = 23.8103,
                        longitude = 90.4125,
                        timestamp = System.currentTimeMillis(),
                        isResolved = false,
                        ttl       = System.currentTimeMillis() + 3_600_000L
                    )
                    emergencyRepository.createEmergencyEvent(sosEvent)
                    sendEffect(EmergencyUiEffect.SosActivated)
                }
            }

            EmergencyUiEvent.CancelSos -> {
                updateState { copy(sosState = SosState.READY) }
                sendEffect(EmergencyUiEffect.SosCancelled)
            }

            is EmergencyUiEvent.ResolveEvent -> {
                viewModelScope.launch {
                    emergencyRepository.resolveEmergencyEvent(event.id)
                    if (currentState.sosState == SosState.ACTIVE) {
                        updateState { copy(sosState = SosState.RESOLVED) }
                    }
                    sendEffect(EmergencyUiEffect.ShowMessage("Emergency resolved"))
                }
            }

            is EmergencyUiEvent.AcknowledgeEvent -> {
                viewModelScope.launch {
                    emergencyRepository.acknowledgeEmergencyEvent(event.id)
                    updateState { copy(sosState = SosState.ACKNOWLEDGED) }
                    sendEffect(EmergencyUiEffect.ShowMessage("Emergency acknowledged"))
                }
            }

            is EmergencyUiEvent.SelectEvent -> updateState { copy(selectedEvent = event.event) }
            EmergencyUiEvent.ClearSelection  -> updateState { copy(selectedEvent = null) }
        }
    }
}
