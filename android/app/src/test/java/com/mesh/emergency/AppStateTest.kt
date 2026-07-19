/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency

import com.mesh.emergency.core.data.AppStateRepositoryImpl
import com.mesh.emergency.core.designsystem.theme.ThemeMode
import com.mesh.emergency.core.domain.AppState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.flowOf
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

/**
 * Unit tests for AppState and AppStateRepositoryImpl.
 * Validates state transitions, default values, and all update methods.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppStateTest {

    @Mock
    private lateinit var mockDataStore: DataStore<Preferences>

    private lateinit var repository: AppStateRepositoryImpl

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        `when`(mockDataStore.data).thenReturn(flowOf(emptyPreferences()))
        repository = AppStateRepositoryImpl(mockDataStore)
        Thread.sleep(100) // Allow async init block running on Dispatchers.IO to complete
    }

    // ── Initial State ──────────────────────────────────────────────────────────

    @Test
    fun appState_initialState_hasCorrectDefaults() = runTest {
        val state = repository.appState.first()
        assertEquals(ThemeMode.SYSTEM, state.themeMode)
        assertEquals("en", state.languageCode)
        assertFalse(state.isOnline)
        assertFalse(state.activeSos)
        assertEquals(0, state.connectedNodeCount)
        assertEquals("NONE", state.activeTransport)
        assertFalse(state.isInitialized)
    }

    // ── Theme Changes ──────────────────────────────────────────────────────────

    @Test
    fun setThemeMode_dark_updatesState() = runTest {
        repository.setThemeMode(ThemeMode.DARK)
        val state = repository.appState.first()
        assertEquals(ThemeMode.DARK, state.themeMode)
    }

    @Test
    fun setThemeMode_light_updatesState() = runTest {
        repository.setThemeMode(ThemeMode.LIGHT)
        val state = repository.appState.first()
        assertEquals(ThemeMode.LIGHT, state.themeMode)
    }

    @Test
    fun setThemeMode_system_retainsSystem() = runTest {
        repository.setThemeMode(ThemeMode.DARK)
        repository.setThemeMode(ThemeMode.SYSTEM)
        val state = repository.appState.first()
        assertEquals(ThemeMode.SYSTEM, state.themeMode)
    }

    // ── Language Changes ───────────────────────────────────────────────────────

    @Test
    fun setLanguage_bangla_updatesCode() = runTest {
        repository.setLanguage("bn")
        val state = repository.appState.first()
        assertEquals("bn", state.languageCode)
    }

    @Test
    fun setLanguage_english_updatesCode() = runTest {
        repository.setLanguage("en")
        val state = repository.appState.first()
        assertEquals("en", state.languageCode)
    }

    // ── Connection Status ──────────────────────────────────────────────────────

    @Test
    fun updateConnectionStatus_online_updatesAllFields() = runTest {
        repository.updateConnectionStatus(isOnline = true, transport = "BLUETOOTH", nodeCount = 3)
        val state = repository.appState.first()
        assertTrue(state.isOnline)
        assertEquals("BLUETOOTH", state.activeTransport)
        assertEquals(3, state.connectedNodeCount)
    }

    @Test
    fun updateConnectionStatus_offline_resetsFields() = runTest {
        repository.updateConnectionStatus(isOnline = true, transport = "LORA", nodeCount = 5)
        repository.updateConnectionStatus(isOnline = false, transport = "NONE", nodeCount = 0)
        val state = repository.appState.first()
        assertFalse(state.isOnline)
        assertEquals("NONE", state.activeTransport)
        assertEquals(0, state.connectedNodeCount)
    }

    // ── Battery ────────────────────────────────────────────────────────────────

    @Test
    fun updateBattery_updatesLevelAndChargingState() = runTest {
        repository.updateBattery(level = 0.45f, isCharging = true)
        val state = repository.appState.first()
        assertEquals(0.45f, state.batteryLevel, 0.01f)
        assertTrue(state.isCharging)
    }

    @Test
    fun updateBattery_critical_isBelow10Percent() = runTest {
        repository.updateBattery(level = 0.08f, isCharging = false)
        val state = repository.appState.first()
        assertTrue(state.batteryLevel < 0.10f)
        assertFalse(state.isCharging)
    }

    // ── SOS ───────────────────────────────────────────────────────────────────

    @Test
    fun setActiveSos_true_updatesState() = runTest {
        repository.setActiveSos(active = true)
        val state = repository.appState.first()
        assertTrue(state.activeSos)
    }

    @Test
    fun setActiveSos_false_clearsState() = runTest {
        repository.setActiveSos(active = true)
        repository.setActiveSos(active = false)
        val state = repository.appState.first()
        assertFalse(state.activeSos)
    }

    // ── Initialization ─────────────────────────────────────────────────────────

    @Test
    fun markInitialized_setsFlag() = runTest {
        assertFalse(repository.appState.first().isInitialized)
        repository.markInitialized()
        assertTrue(repository.appState.first().isInitialized)
    }

    // ── State is a data class ─────────────────────────────────────────────────

    @Test
    fun appState_dataClass_copiesCorrectly() {
        val base = AppState()
        val modified = base.copy(languageCode = "bn", themeMode = ThemeMode.DARK)
        assertEquals("bn", modified.languageCode)
        assertEquals(ThemeMode.DARK, modified.themeMode)
        assertEquals(base.isOnline, modified.isOnline) // unchanged fields preserved
    }
}
