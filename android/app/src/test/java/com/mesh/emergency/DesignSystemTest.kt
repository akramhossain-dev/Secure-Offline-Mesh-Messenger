/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency

import com.mesh.emergency.core.designsystem.component.AlertPriorityLevel
import com.mesh.emergency.core.designsystem.component.UiState
import com.mesh.emergency.core.designsystem.theme.AuroraColors
import com.mesh.emergency.core.designsystem.theme.DarkAuroraColors
import com.mesh.emergency.core.designsystem.theme.LightAuroraColors
import com.mesh.emergency.core.designsystem.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for design system tokens — theme configs, Aurora colors, and UI state classes.
 * These are pure JVM tests with no Android framework dependency.
 */
class DesignSystemTest {

    // ── Theme Config ──────────────────────────────────────────────────────────

    @Test
    fun themeMode_fromString_returnsCorrectMode() {
        assertEquals(ThemeMode.DARK,   ThemeMode.fromString("dark"))
        assertEquals(ThemeMode.LIGHT,  ThemeMode.fromString("light"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromString("system"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromString("unknown_value"))
    }

    @Test
    fun themeMode_default_isSystem() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.DEFAULT)
    }

    // ── Aurora Color Tokens ───────────────────────────────────────────────────

    @Test
    fun auroraColors_dark_hasNonZeroAlpha() {
        val dark: AuroraColors = DarkAuroraColors
        assertTrue(dark.glassBackground.alpha > 0f)
        assertTrue(dark.glassSurface.alpha > 0f)
        assertTrue(dark.glassEmergency.alpha > 0f)
        assertTrue(dark.glassBorder.alpha > 0f)
    }

    @Test
    fun auroraColors_light_hasNonZeroAlpha() {
        val light: AuroraColors = LightAuroraColors
        assertTrue(light.glassBackground.alpha > 0f)
        assertTrue(light.glassEmergency.alpha > 0f)
    }

    @Test
    fun auroraColors_dark_glassSurface_isTranslucent() {
        // Dark glass surface should be at most 15% opaque
        assertTrue(DarkAuroraColors.glassSurface.alpha <= 0.15f)
    }

    @Test
    fun auroraColors_light_glassBackground_isHighlyOpaque() {
        // Light glass background should be at least 75% opaque (readable)
        assertTrue(LightAuroraColors.glassBackground.alpha >= 0.75f)
    }

    @Test
    fun auroraColors_hasDistinctGradientEndpoints() {
        val dark = DarkAuroraColors
        assertNotNull(dark.auroraStart)
        assertNotNull(dark.auroraMid)
        assertNotNull(dark.auroraEnd)
        // All three gradient stops should be different colors
        assertTrue(dark.auroraStart != dark.auroraMid)
        assertTrue(dark.auroraMid  != dark.auroraEnd)
    }

    // ── UiState Sealed Class ──────────────────────────────────────────────────

    @Test
    fun uiState_loading_isCorrectType() {
        val state: UiState<String> = UiState.Loading
        assertTrue(state is UiState.Loading)
    }

    @Test
    fun uiState_empty_isCorrectType() {
        val state: UiState<List<Int>> = UiState.Empty
        assertTrue(state is UiState.Empty)
    }

    @Test
    fun uiState_success_holdsData() {
        val state = UiState.Success(data = listOf(1, 2, 3))
        assertTrue(state is UiState.Success)
        assertEquals(listOf(1, 2, 3), (state as UiState.Success).data)
    }

    @Test
    fun uiState_error_holdsMessage() {
        val state = UiState.Error(message = "Network failure", retryable = true)
        assertTrue(state is UiState.Error)
        assertEquals("Network failure", (state as UiState.Error).message)
        assertTrue(state.retryable)
    }

    @Test
    fun uiState_offline_isCorrectType() {
        val state: UiState<Unit> = UiState.Offline
        assertTrue(state is UiState.Offline)
    }

    // ── Alert Priority Levels ─────────────────────────────────────────────────

    @Test
    fun alertPriorityLevel_allCasesPresent() {
        val levels = AlertPriorityLevel.entries
        assertTrue(levels.contains(AlertPriorityLevel.CRITICAL))
        assertTrue(levels.contains(AlertPriorityLevel.HIGH))
        assertTrue(levels.contains(AlertPriorityLevel.NORMAL))
        assertTrue(levels.contains(AlertPriorityLevel.LOW))
    }
}
