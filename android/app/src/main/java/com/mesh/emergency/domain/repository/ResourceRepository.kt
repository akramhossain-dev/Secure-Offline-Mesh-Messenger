/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.domain.repository

import com.mesh.emergency.core.common.result.Result
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface regulating emergency resources persistence.
 */
interface ResourceRepository {
    /** Stream of all saved resource records. */
    fun getResources(): Flow<Result<List<ResourceDomainModel>>>

    /** Caches a resource record in local database. */
    suspend fun saveResource(resource: ResourceDomainModel): Result<Unit>

    /** Removes a resource record. */
    suspend fun deleteResource(id: String): Result<Unit>
}

/**
 * Domain model representing a shared resource or request.
 */
data class ResourceDomainModel(
    val id: String,
    val ownerId: String,
    val name: String,
    val type: String,
    val quantity: Int,
    val latitude: Double,
    val longitude: Double,
    val description: String,
    val status: String,
    val privacy: String,
    val ttl: Long
)
