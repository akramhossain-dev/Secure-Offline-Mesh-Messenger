/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
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
 * Persists themeMode, languageCode, chatHeads, and notification preferences across process restarts via [DataStore].
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

            val chatHeads = prefs[KEY_CHAT_HEADS] ?: true
            val bubbles = prefs[KEY_BUBBLES] ?: true
            val floatingChat = prefs[KEY_FLOATING_CHAT] ?: true
            val sound = prefs[KEY_SOUND] ?: true
            val vibration = prefs[KEY_VIBRATION] ?: true
            val popup = prefs[KEY_POPUP] ?: true

            _appState.update {
                it.copy(
                    themeMode = restoredTheme,
                    languageCode = restoredLanguage,
                    chatHeadsEnabled = chatHeads,
                    bubblesEnabled = bubbles,
                    floatingChatEnabled = floatingChat,
                    soundEnabled = sound,
                    vibrationEnabled = vibration,
                    popupPreviewEnabled = popup
                )
            }
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

    override suspend fun setChatHeadsEnabled(enabled: Boolean) {
        _appState.update { it.copy(chatHeadsEnabled = enabled) }
        dataStore.edit { prefs -> prefs[KEY_CHAT_HEADS] = enabled }
    }

    override suspend fun setBubblesEnabled(enabled: Boolean) {
        _appState.update { it.copy(bubblesEnabled = enabled) }
        dataStore.edit { prefs -> prefs[KEY_BUBBLES] = enabled }
    }

    override suspend fun setFloatingChatEnabled(enabled: Boolean) {
        _appState.update { it.copy(floatingChatEnabled = enabled) }
        dataStore.edit { prefs -> prefs[KEY_FLOATING_CHAT] = enabled }
    }

    override suspend fun setSoundEnabled(enabled: Boolean) {
        _appState.update { it.copy(soundEnabled = enabled) }
        dataStore.edit { prefs -> prefs[KEY_SOUND] = enabled }
    }

    override suspend fun setVibrationEnabled(enabled: Boolean) {
        _appState.update { it.copy(vibrationEnabled = enabled) }
        dataStore.edit { prefs -> prefs[KEY_VIBRATION] = enabled }
    }

    override suspend fun setPopupPreviewEnabled(enabled: Boolean) {
        _appState.update { it.copy(popupPreviewEnabled = enabled) }
        dataStore.edit { prefs -> prefs[KEY_POPUP] = enabled }
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
        private val KEY_THEME         = stringPreferencesKey("app_theme_mode")
        private val KEY_LANGUAGE      = stringPreferencesKey("app_language_code")
        private val KEY_CHAT_HEADS    = booleanPreferencesKey("chat_heads_enabled")
        private val KEY_BUBBLES       = booleanPreferencesKey("bubbles_enabled")
        private val KEY_FLOATING_CHAT = booleanPreferencesKey("floating_chat_enabled")
        private val KEY_SOUND         = booleanPreferencesKey("sound_enabled")
        private val KEY_VIBRATION     = booleanPreferencesKey("vibration_enabled")
        private val KEY_POPUP         = booleanPreferencesKey("popup_preview_enabled")
    }
}
