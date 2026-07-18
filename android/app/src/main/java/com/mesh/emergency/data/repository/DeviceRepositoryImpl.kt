/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.repository

import com.mesh.emergency.core.common.result.Result
import com.mesh.emergency.domain.repository.DeviceDomainModel
import com.mesh.emergency.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data layer implementation of [DeviceRepository] returning stubbed values.
 */
@Singleton
class DeviceRepositoryImpl @Inject constructor() : DeviceRepository {

    override fun getDiscoveredDevices(): Flow<Result<List<DeviceDomainModel>>> {
        return flowOf(Result.Success(emptyList()))
    }

    override suspend fun startScan(): Result<Unit> {
        return Result.Success(Unit)
    }

    override suspend fun stopScan(): Result<Unit> {
        return Result.Success(Unit)
    }
}
