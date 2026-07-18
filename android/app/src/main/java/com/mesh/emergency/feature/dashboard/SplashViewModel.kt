/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.feature.dashboard

import androidx.lifecycle.viewModelScope
import com.mesh.emergency.core.domain.AppStateRepository
import com.mesh.emergency.core.presentation.base.BaseUiEffect
import com.mesh.emergency.core.presentation.base.BaseUiEvent
import com.mesh.emergency.core.presentation.base.BaseUiState
import com.mesh.emergency.core.presentation.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── State ────────────────────────────────────────────────────────────────────
data class SplashUiState(
    val isLoading: Boolean = true,
    val appName: String = "OfflineMesh",
    val version: String = "v0.1.0"
) : BaseUiState

// ── Events ───────────────────────────────────────────────────────────────────
sealed interface SplashUiEvent : BaseUiEvent {
    data object InitializationComplete : SplashUiEvent
}

// ── Effects ───────────────────────────────────────────────────────────────────
sealed interface SplashUiEffect : BaseUiEffect {
    data object NavigateToHome : SplashUiEffect
}

// ── ViewModel ─────────────────────────────────────────────────────────────────
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val appStateRepository: AppStateRepository
) : BaseViewModel<SplashUiState, SplashUiEvent, SplashUiEffect>(SplashUiState()) {

    init {
        initialize()
    }

    private fun initialize() {
        viewModelScope.launch {
            // Simulate initialization — load persisted theme / language etc.
            delay(1200L)
            updateState { copy(isLoading = false) }
            sendEffect(SplashUiEffect.NavigateToHome)
        }
    }

    override fun onEvent(event: SplashUiEvent) {
        when (event) {
            SplashUiEvent.InitializationComplete -> sendEffect(SplashUiEffect.NavigateToHome)
        }
    }
}
