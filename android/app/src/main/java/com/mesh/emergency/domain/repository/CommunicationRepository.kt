/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.domain.repository

import com.mesh.emergency.core.common.result.Result
import com.mesh.emergency.core.model.BaseModel
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface defining mesh network diagnostics telemetry access.
 */
interface CommunicationRepository {
    /** Emits current mesh status updates. */
    fun getMeshStatus(): Flow<Result<MeshStatusDomainModel>>

    /** Dispatches configuration commands to active mesh controllers. */
    suspend fun sendSystemCommand(command: String): Result<Unit>
}

/**
 * Domain model representing mesh and node diagnostics.
 */
data class MeshStatusDomainModel(
    override val id: String,
    val activeNodes: Int,
    val loraSignalStrength: Int,
    val batteryLevel: Float
) : BaseModel
