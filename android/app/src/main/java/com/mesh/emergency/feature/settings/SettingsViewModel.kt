/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.feature.settings

import androidx.lifecycle.viewModelScope
import com.mesh.emergency.core.designsystem.theme.ThemeMode
import com.mesh.emergency.core.domain.AppStateRepository
import com.mesh.emergency.core.presentation.base.BaseUiEffect
import com.mesh.emergency.core.presentation.base.BaseUiEvent
import com.mesh.emergency.core.presentation.base.BaseUiState
import com.mesh.emergency.core.presentation.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── State ─────────────────────────────────────────────────────────────────────
data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val languageCode: String = "en",
    val debugModeEnabled: Boolean = false,
    val encryptionEnabled: Boolean = true,
    val storageUsageMb: Float = 0f,
    val appVersion: String = "0.1.0-debug"
) : BaseUiState

// ── Events ────────────────────────────────────────────────────────────────────
sealed interface SettingsUiEvent : BaseUiEvent {
    data class ChangeTheme(val mode: ThemeMode) : SettingsUiEvent
    data class ChangeLanguage(val code: String)  : SettingsUiEvent
    data class ToggleDebugMode(val enabled: Boolean) : SettingsUiEvent
    data object ClearLogs : SettingsUiEvent
}

// ── Effects ───────────────────────────────────────────────────────────────────
sealed interface SettingsUiEffect : BaseUiEffect {
    data class ShowSnackbar(val message: String) : SettingsUiEffect
}

// ── ViewModel ─────────────────────────────────────────────────────────────────
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appStateRepository: AppStateRepository
) : BaseViewModel<SettingsUiState, SettingsUiEvent, SettingsUiEffect>(SettingsUiState()) {

    init {
        observeAppState()
    }

    private fun observeAppState() {
        viewModelScope.launch {
            appStateRepository.appState.collect { state ->
                updateState { copy(themeMode = state.themeMode, languageCode = state.languageCode) }
            }
        }
    }

    override fun onEvent(event: SettingsUiEvent) {
        when (event) {
            is SettingsUiEvent.ChangeTheme -> {
                viewModelScope.launch {
                    appStateRepository.setThemeMode(event.mode)
                    sendEffect(SettingsUiEffect.ShowSnackbar("Theme changed to ${event.mode.name}"))
                }
            }
            is SettingsUiEvent.ChangeLanguage -> {
                viewModelScope.launch {
                    appStateRepository.setLanguage(event.code)
                    sendEffect(SettingsUiEffect.ShowSnackbar("Language updated"))
                }
            }
            is SettingsUiEvent.ToggleDebugMode -> {
                updateState { copy(debugModeEnabled = event.enabled) }
            }
            SettingsUiEvent.ClearLogs -> {
                sendEffect(SettingsUiEffect.ShowSnackbar("Diagnostic logs cleared"))
            }
        }
    }
}
