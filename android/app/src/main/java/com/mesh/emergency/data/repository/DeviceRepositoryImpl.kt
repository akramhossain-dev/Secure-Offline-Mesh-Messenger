/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.repository

import com.mesh.emergency.core.common.result.Result
import com.mesh.emergency.data.local.LocalDataSource
import com.mesh.emergency.data.local.entity.DeviceEntity
import com.mesh.emergency.domain.repository.DeviceDomainModel
import com.mesh.emergency.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository regulating scans lists persistence using Room database cache.
 */
@Singleton
class DeviceRepositoryImpl @Inject constructor(
    private val localDataSource: LocalDataSource
) : DeviceRepository {

    override fun getDiscoveredDevices(): Flow<Result<List<DeviceDomainModel>>> {
        return localDataSource.getDevices().map { entities ->
            Result.Success(entities.map { it.toDomain() })
        }
    }

    override suspend fun startScan(): Result<Unit> {
        return Result.Success(Unit) // Handled by BLE adapter service in Phase A2+
    }

    override suspend fun stopScan(): Result<Unit> {
        return try {
            localDataSource.clearAllDevices()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}

private fun DeviceEntity.toDomain(): DeviceDomainModel = DeviceDomainModel(
    id = entityId,
    name = name,
    rssi = rssi,
    lastSeen = lastSeen
)
