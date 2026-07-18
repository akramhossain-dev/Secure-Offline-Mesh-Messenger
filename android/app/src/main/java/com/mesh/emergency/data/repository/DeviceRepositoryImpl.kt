/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.repository

import com.mesh.emergency.core.common.result.Result
import com.mesh.emergency.data.local.LocalDataSource
import com.mesh.emergency.data.local.entity.DbTrustStatus
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
        return Result.Success(Unit)
    }

    override suspend fun stopScan(): Result<Unit> {
        return try {
            localDataSource.clearAllDevices()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun updateTrustStatus(deviceId: String, status: String): Result<Unit> {
        return try {
            val existing = localDataSource.getDeviceById(deviceId)
            if (existing != null) {
                val updated = existing.copy(trustStatus = DbTrustStatus.valueOf(status))
                localDataSource.insertDevice(updated)
                Result.Success(Unit)
            } else {
                Result.Error(Exception("Device not found"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun pairDevice(
        deviceId: String,
        name: String,
        deviceType: String,
        platformInfo: String
    ): Result<Unit> {
        return try {
            val now = System.currentTimeMillis()
            val device = DeviceEntity(
                entityId = deviceId,
                name = name,
                rssi = -100,
                lastSeen = now,
                deviceType = deviceType,
                platformInfo = platformInfo,
                createdTime = now,
                lastActiveTime = now,
                trustStatus = DbTrustStatus.TRUSTED
            )
            localDataSource.insertDevice(device)
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
