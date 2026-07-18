/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.repository

import com.mesh.emergency.core.common.result.Result
import com.mesh.emergency.core.storage.StorageManager
import com.mesh.emergency.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository regulating preferences persistence using [StorageManager].
 */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val storageManager: StorageManager
) : SettingsRepository {

    override fun getThemeMode(): Flow<Result<String>> {
        return storageManager.getString("app_theme_mode", "SYSTEM").map { mode ->
            Result.Success(mode)
        }
    }

    override suspend fun setThemeMode(mode: String): Result<Unit> {
        return try {
            storageManager.saveString("app_theme_mode", mode)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override fun getLanguageTag(): Flow<Result<String>> {
        return storageManager.getString("app_language", "en").map { tag ->
            Result.Success(tag)
        }
    }

    override suspend fun setLanguageTag(tag: String): Result<Unit> {
        return try {
            storageManager.saveString("app_language", tag)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
