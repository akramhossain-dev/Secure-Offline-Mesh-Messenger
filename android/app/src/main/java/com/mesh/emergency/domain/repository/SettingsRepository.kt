/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.domain.repository

import com.mesh.emergency.core.common.result.Result
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface regulating system-level settings and preferences.
 */
interface SettingsRepository {
    /** Emits active theme mode string token. */
    fun getThemeMode(): Flow<Result<String>>

    /** Updates active theme mode token. */
    suspend fun setThemeMode(mode: String): Result<Unit>

    /** Emits active language locale identifier. */
    fun getLanguageTag(): Flow<Result<String>>

    /** Updates active language locale setting. */
    suspend fun setLanguageTag(tag: String): Result<Unit>
}
