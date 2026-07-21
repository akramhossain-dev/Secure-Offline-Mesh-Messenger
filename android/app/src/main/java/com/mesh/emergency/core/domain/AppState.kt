/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.domain

import com.mesh.emergency.core.designsystem.theme.ThemeMode
import kotlinx.coroutines.flow.StateFlow

// ─────────────────────────────────────────────────────────────────────────────
// AppState — global application state model
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Represents the complete application-level state observed by the root composable.
 * Populated from local DataStore preferences and system-level broadcasts.
 */
data class AppState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val languageCode: String = "en",
    val isOnline: Boolean = false,
    val batteryLevel: Float = 1.0f,
    val isCharging: Boolean = false,
    val activeSos: Boolean = false,
    val connectedNodeCount: Int = 0,
    val activeTransport: String = "NONE",
    val isInitialized: Boolean = false,

    // ── Notification & Overlay Preferences ──────────────────────────────────
    val chatHeadsEnabled: Boolean = true,
    val bubblesEnabled: Boolean = true,
    val floatingChatEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val popupPreviewEnabled: Boolean = true
)

/**
 * Contract for providing and mutating the global [AppState].
 */
interface AppStateRepository {
    val appState: StateFlow<AppState>
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setLanguage(code: String)
    fun updateConnectionStatus(isOnline: Boolean, transport: String, nodeCount: Int)
    fun updateBattery(level: Float, isCharging: Boolean)
    fun setActiveSos(active: Boolean)

    // Notification Preference Updaters
    suspend fun setChatHeadsEnabled(enabled: Boolean)
    suspend fun setBubblesEnabled(enabled: Boolean)
    suspend fun setFloatingChatEnabled(enabled: Boolean)
    suspend fun setSoundEnabled(enabled: Boolean)
    suspend fun setVibrationEnabled(enabled: Boolean)
    suspend fun setPopupPreviewEnabled(enabled: Boolean)
}
