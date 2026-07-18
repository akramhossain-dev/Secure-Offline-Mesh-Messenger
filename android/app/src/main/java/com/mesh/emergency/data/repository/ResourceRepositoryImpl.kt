/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.repository

import com.mesh.emergency.core.common.result.Result
import com.mesh.emergency.data.local.LocalDataSource
import com.mesh.emergency.data.local.entity.DbResourcePrivacy
import com.mesh.emergency.data.local.entity.DbResourceStatus
import com.mesh.emergency.data.local.entity.ResourceEntity
import com.mesh.emergency.domain.repository.ResourceDomainModel
import com.mesh.emergency.domain.repository.ResourceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [ResourceRepository] mapping Room database logs.
 */
@Singleton
class ResourceRepositoryImpl @Inject constructor(
    private val localDataSource: LocalDataSource
) : ResourceRepository {

    override fun getResources(): Flow<Result<List<ResourceDomainModel>>> {
        return localDataSource.getResources().map { entities ->
            Result.Success(entities.map { it.toDomain() })
        }
    }

    override suspend fun saveResource(resource: ResourceDomainModel): Result<Unit> {
        return try {
            val entity = ResourceEntity(
                entityId = resource.id,
                ownerId = resource.ownerId,
                name = resource.name,
                type = resource.type,
                quantity = resource.quantity,
                latitude = resource.latitude,
                longitude = resource.longitude,
                description = resource.description,
                availabilityStatus = DbResourceStatus.valueOf(resource.status),
                privacyLevel = DbResourcePrivacy.valueOf(resource.privacy),
                ttl = resource.ttl
            )
            localDataSource.insertResource(entity)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun deleteResource(id: String): Result<Unit> {
        return try {
            // Mapping deletion operations to database sweeps
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}

private fun ResourceEntity.toDomain(): ResourceDomainModel = ResourceDomainModel(
    id = entityId,
    ownerId = ownerId,
    name = name,
    type = type,
    quantity = quantity,
    latitude = latitude,
    longitude = longitude,
    description = description,
    status = availabilityStatus.name,
    privacy = privacyLevel.name,
    ttl = ttl
)
