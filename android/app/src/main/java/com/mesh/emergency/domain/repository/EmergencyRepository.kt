/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.domain.repository

import com.mesh.emergency.core.common.result.Result
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface regulating emergency SOS logs persistence.
 */
interface EmergencyRepository {
    /** Stream of all saved emergency beacons logs. */
    fun getEmergencyEvents(): Flow<Result<List<EmergencyDomainModel>>>

    /** Caches an emergency SOS event. */
    suspend fun insertEmergency(event: EmergencyDomainModel): Result<Unit>

    /** Modifies the lifecycle state of an SOS event. */
    suspend fun updateEmergencyStatus(id: String, status: String): Result<Unit>
}

/**
 * Domain model representing an emergency SOS alert.
 */
data class EmergencyDomainModel(
    val id: String,
    val senderId: String,
    val latitude: Double,
    val longitude: Double,
    val message: String,
    val timestamp: Long,
    val status: String,
    val type: String
)
