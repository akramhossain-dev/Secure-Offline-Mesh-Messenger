/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.emergency

import com.mesh.emergency.core.common.result.Result
import com.mesh.emergency.core.communication.CommunicationManager
import com.mesh.emergency.core.emergency.EmergencyManager
import com.mesh.emergency.data.local.LocalDataSource
import com.mesh.emergency.data.local.entity.DbEmergencyStatus
import com.mesh.emergency.data.local.entity.DbEmergencyType
import com.mesh.emergency.data.local.entity.DbMessagePriority
import com.mesh.emergency.data.local.entity.EmergencyEventEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [EmergencyManager] coordinating local SOS event pipelines.
 */
@Singleton
class EmergencyManagerImpl @Inject constructor(
    private val localDataSource: LocalDataSource,
    private val communicationManager: CommunicationManager
) : EmergencyManager {

    private val _isEmergencyMode = MutableStateFlow(false)
    override val isEmergencyMode: StateFlow<Boolean> = _isEmergencyMode.asStateFlow()

    override suspend fun triggerSOS(latitude: Double, longitude: Double, message: String): Result<EmergencyEventEntity> {
        return try {
            _isEmergencyMode.value = true
            val now = System.currentTimeMillis()
            val event = EmergencyEventEntity(
                entityId = UUID.randomUUID().toString(),
                senderId = "local_user_id",
                latitude = latitude,
                longitude = longitude,
                message = message,
                timestamp = now,
                isResolved = false,
                emergencyType = DbEmergencyType.SOS,
                priority = DbMessagePriority.CRITICAL,
                status = DbEmergencyStatus.CREATED,
                ttl = now + 86400000L * 7 // SOS has 7 days TTL
            )
            localDataSource.insertEmergencyEvent(event)

            // Update status to SENDING
            val sendingEvent = event.copy(status = DbEmergencyStatus.SENDING)
            localDataSource.insertEmergencyEvent(sendingEvent)

            val payload = "[SOS] Lat:$latitude, Lng:$longitude Msg:$message".toByteArray()
            val result = communicationManager.sendMessage(payload)

            val finalEvent = when (result) {
                is Result.Success -> sendingEvent.copy(status = DbEmergencyStatus.BROADCASTING)
                is Result.Error   -> sendingEvent.copy(status = DbEmergencyStatus.CREATED)
                is Result.Loading -> sendingEvent
            }
            localDataSource.insertEmergencyEvent(finalEvent)

            Result.Success(finalEvent)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun acknowledgeSOS(emergencyId: String, responseType: String): Result<Unit> {
        return try {
            val list = localDataSource.getEmergencyEvents().first()
            val match = list.firstOrNull { it.entityId == emergencyId }
            if (match != null) {
                val updated = match.copy(status = DbEmergencyStatus.ACKNOWLEDGED)
                localDataSource.insertEmergencyEvent(updated)
                Result.Success(Unit)
            } else {
                Result.Error(Exception("Emergency event not found"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun resolveEmergency(emergencyId: String): Result<Unit> {
        return try {
            val list = localDataSource.getEmergencyEvents().first()
            val match = list.firstOrNull { it.entityId == emergencyId }
            if (match != null) {
                val updated = match.copy(status = DbEmergencyStatus.RESOLVED, isResolved = true)
                localDataSource.insertEmergencyEvent(updated)
                _isEmergencyMode.value = false
                Result.Success(Unit)
            } else {
                Result.Error(Exception("Emergency event not found"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun cancelEmergency(emergencyId: String): Result<Unit> {
        return try {
            val list = localDataSource.getEmergencyEvents().first()
            val match = list.firstOrNull { it.entityId == emergencyId }
            if (match != null) {
                val updated = match.copy(status = DbEmergencyStatus.CANCELLED)
                localDataSource.insertEmergencyEvent(updated)
                _isEmergencyMode.value = false
                Result.Success(Unit)
            } else {
                Result.Error(Exception("Emergency event not found"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
