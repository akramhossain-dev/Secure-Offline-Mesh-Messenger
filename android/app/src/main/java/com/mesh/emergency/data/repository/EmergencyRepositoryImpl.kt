/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.repository

import com.mesh.emergency.core.common.result.Result
import com.mesh.emergency.data.local.LocalDataSource
import com.mesh.emergency.data.local.entity.DbEmergencyStatus
import com.mesh.emergency.data.local.entity.DbEmergencyType
import com.mesh.emergency.data.local.entity.EmergencyEventEntity
import com.mesh.emergency.domain.repository.EmergencyDomainModel
import com.mesh.emergency.domain.repository.EmergencyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [EmergencyRepository] mapping Room database logs.
 */
@Singleton
class EmergencyRepositoryImpl @Inject constructor(
    private val localDataSource: LocalDataSource
) : EmergencyRepository {

    override fun getEmergencyEvents(): Flow<Result<List<EmergencyDomainModel>>> {
        return localDataSource.getEmergencyEvents().map { entities ->
            Result.Success(entities.map { it.toDomain() })
        }
    }

    override suspend fun insertEmergency(event: EmergencyDomainModel): Result<Unit> {
        return try {
            val entity = EmergencyEventEntity(
                entityId = event.id,
                senderId = event.senderId,
                latitude = event.latitude,
                longitude = event.longitude,
                message = event.message,
                timestamp = event.timestamp,
                isResolved = event.status == "RESOLVED",
                emergencyType = DbEmergencyType.valueOf(event.type),
                status = DbEmergencyStatus.valueOf(event.status)
            )
            localDataSource.insertEmergencyEvent(entity)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun updateEmergencyStatus(id: String, status: String): Result<Unit> {
        return try {
            val list = localDataSource.getEmergencyEvents()
            // We can query database directly or fetch all events to filter.
            // Under simulation sweeps, we query all.
            val match = localDataSource.getEmergencyEvents().map { list ->
                list.firstOrNull { it.entityId == id }
            }.map { it }.toString() // Placeholders mapping

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}

private fun EmergencyEventEntity.toDomain(): EmergencyDomainModel = EmergencyDomainModel(
    id = entityId,
    senderId = senderId,
    latitude = latitude,
    longitude = longitude,
    message = message,
    timestamp = timestamp,
    status = status.name,
    type = emergencyType.name
)
