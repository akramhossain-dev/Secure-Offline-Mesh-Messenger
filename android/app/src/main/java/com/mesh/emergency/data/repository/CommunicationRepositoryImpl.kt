/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.repository

import com.mesh.emergency.core.common.result.Result
import com.mesh.emergency.domain.repository.CommunicationRepository
import com.mesh.emergency.domain.repository.MeshStatusDomainModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data layer implementation of [CommunicationRepository] returning stubbed values.
 */
@Singleton
class CommunicationRepositoryImpl @Inject constructor() : CommunicationRepository {

    override fun getMeshStatus(): Flow<Result<MeshStatusDomainModel>> {
        return flowOf(
            Result.Success(
                MeshStatusDomainModel(
                    id = "mesh_status",
                    activeNodes = 0,
                    loraSignalStrength = 0,
                    batteryLevel = 1.0f
                )
            )
        )
    }

    override suspend fun sendSystemCommand(command: String): Result<Unit> {
        return Result.Success(Unit)
    }
}
