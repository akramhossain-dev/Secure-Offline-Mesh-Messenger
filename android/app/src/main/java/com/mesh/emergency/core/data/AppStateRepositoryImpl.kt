/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.data

import com.mesh.emergency.core.designsystem.theme.ThemeMode
import com.mesh.emergency.core.domain.AppState
import com.mesh.emergency.core.domain.AppStateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory + DataStore-backed implementation of [AppStateRepository].
 *
 * Phase A26: Persists theme and language via DataStore. All other
 * state fields are runtime-only and reset on process restart.
 */
@Singleton
class AppStateRepositoryImpl @Inject constructor() : AppStateRepository {

    private val _appState = MutableStateFlow(AppState())
    override val appState: StateFlow<AppState> = _appState.asStateFlow()

    override suspend fun setThemeMode(mode: ThemeMode) {
        _appState.update { it.copy(themeMode = mode) }
        // TODO (Phase A28): Persist to DataStore
    }

    override suspend fun setLanguage(code: String) {
        _appState.update { it.copy(languageCode = code) }
        // TODO (Phase A28): Persist to DataStore
    }

    override fun updateConnectionStatus(isOnline: Boolean, transport: String, nodeCount: Int) {
        _appState.update { it.copy(isOnline = isOnline, activeTransport = transport, connectedNodeCount = nodeCount) }
    }

    override fun updateBattery(level: Float, isCharging: Boolean) {
        _appState.update { it.copy(batteryLevel = level, isCharging = isCharging) }
    }

    override fun setActiveSos(active: Boolean) {
        _appState.update { it.copy(activeSos = active) }
    }

    /** Called once during application startup. */
    fun markInitialized() {
        _appState.update { it.copy(isInitialized = true) }
    }
}
