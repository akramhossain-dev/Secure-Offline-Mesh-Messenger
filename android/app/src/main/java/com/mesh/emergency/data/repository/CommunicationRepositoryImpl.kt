/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.repository

import com.mesh.emergency.core.common.result.Result
import com.mesh.emergency.data.local.LocalDataSource
import com.mesh.emergency.data.local.entity.NetworkNodeEntity
import com.mesh.emergency.domain.repository.CommunicationRepository
import com.mesh.emergency.domain.repository.MeshStatusDomainModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository regulating mesh diagnostic updates from local data source.
 */
@Singleton
class CommunicationRepositoryImpl @Inject constructor(
    private val localDataSource: LocalDataSource
) : CommunicationRepository {

    override fun getMeshStatus(): Flow<Result<MeshStatusDomainModel>> {
        return localDataSource.getNetworkNodes().map { nodes ->
            val active = nodes.size
            val maxRssi = nodes.maxOfOrNull { it.rssi } ?: -100
            val avgBattery = nodes.map { it.batteryLevel }.average().toFloat().coerceIn(0f, 1f)

            Result.Success(
                MeshStatusDomainModel(
                    id = "mesh_status",
                    activeNodes = active,
                    loraSignalStrength = maxRssi,
                    batteryLevel = if (avgBattery.isNaN()) 1.0f else avgBattery
                )
            )
        }
    }

    override suspend fun sendSystemCommand(command: String): Result<Unit> {
        return try {
            // Commands are routed directly to Esp32 transport in later phases
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
