/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mesh.emergency.core.designsystem.theme.ThemeMode
import com.mesh.emergency.core.domain.AppState
import com.mesh.emergency.core.domain.AppStateRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataStore-backed implementation of [AppStateRepository].
 *
 * Persists [themeMode] and [languageCode] across process restarts via [DataStore].
 * All other state fields (battery, connectivity, SOS) are runtime-only and reset on restart.
 */
@Singleton
class AppStateRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : AppStateRepository {

    private val scope = CoroutineScope(Dispatchers.IO)

    private val _appState = MutableStateFlow(AppState())
    override val appState: StateFlow<AppState> = _appState.asStateFlow()

    init {
        // Restore persisted preferences on startup
        scope.launch {
            val prefs = dataStore.data.first()
            val restoredTheme = prefs[KEY_THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM
            val restoredLanguage = prefs[KEY_LANGUAGE] ?: "en"
            _appState.update { it.copy(themeMode = restoredTheme, languageCode = restoredLanguage) }
        }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        _appState.update { it.copy(themeMode = mode) }
        dataStore.edit { prefs -> prefs[KEY_THEME] = mode.name }
    }

    override suspend fun setLanguage(code: String) {
        _appState.update { it.copy(languageCode = code) }
        dataStore.edit { prefs -> prefs[KEY_LANGUAGE] = code }
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

    companion object {
        private val KEY_THEME    = stringPreferencesKey("app_theme_mode")
        private val KEY_LANGUAGE = stringPreferencesKey("app_language_code")
    }
}
