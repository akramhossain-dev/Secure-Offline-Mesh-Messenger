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
import com.mesh.emergency.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
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
    data object NavigateToHome       : SplashUiEffect
    data object NavigateToOnboarding : SplashUiEffect
}

// ── ViewModel ─────────────────────────────────────────────────────────────────
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val appStateRepository: AppStateRepository,
    private val userRepository: UserRepository
) : BaseViewModel<SplashUiState, SplashUiEvent, SplashUiEffect>(SplashUiState()) {

    init {
        initialize()
    }

    private fun initialize() {
        viewModelScope.launch {
            // Apply delay to allow system layout initialization
            delay(1600L)
            updateState { copy(isLoading = false) }

            // Check if profile exists in local DB
            val hasProfile = try {
                val result = userRepository.getCurrentUser().first()
                result is com.mesh.emergency.core.common.result.Result.Success
            } catch (e: Exception) {
                false
            }

            if (hasProfile) {
                sendEffect(SplashUiEffect.NavigateToHome)
            } else {
                sendEffect(SplashUiEffect.NavigateToOnboarding)
            }
        }
    }

    override fun onEvent(event: SplashUiEvent) {
        when (event) {
            SplashUiEvent.InitializationComplete -> {
                // Check completed in initialize()
            }
        }
    }
}
