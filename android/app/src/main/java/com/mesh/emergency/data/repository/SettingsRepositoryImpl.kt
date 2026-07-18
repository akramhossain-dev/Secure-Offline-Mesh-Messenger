/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.repository

import com.mesh.emergency.core.common.result.Result
import com.mesh.emergency.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data layer implementation of [SettingsRepository] returning stubbed values.
 */
@Singleton
class SettingsRepositoryImpl @Inject constructor() : SettingsRepository {

    override fun getThemeMode(): Flow<Result<String>> {
        return flowOf(Result.Success("SYSTEM"))
    }

    override suspend fun setThemeMode(mode: String): Result<Unit> {
        return Result.Success(Unit)
    }

    override fun getLanguageTag(): Flow<Result<String>> {
        return flowOf(Result.Success("en"))
    }

    override suspend fun setLanguageTag(tag: String): Result<Unit> {
        return Result.Success(Unit)
    }
}
