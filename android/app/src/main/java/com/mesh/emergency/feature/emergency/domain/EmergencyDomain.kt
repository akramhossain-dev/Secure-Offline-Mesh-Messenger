/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.feature.emergency.domain

import com.mesh.emergency.data.local.entity.DbEmergencyStatus
import com.mesh.emergency.data.local.entity.DbEmergencyType
import com.mesh.emergency.data.local.entity.DbMessagePriority
import com.mesh.emergency.data.local.entity.EmergencyEventEntity
import kotlinx.coroutines.flow.Flow

// ─────────────────────────────────────────────────────────────────────────────
// Emergency Domain Models
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Presentation-layer representation of an emergency event.
 * Decoupled from the Room entity to allow independent UI evolution.
 */
data class EmergencyEvent(
    val id: String,
    val type: DbEmergencyType,
    val priority: DbMessagePriority,
    val status: DbEmergencyStatus,
    val senderId: String,
    val message: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val isResolved: Boolean,
    val ttl: Long
)

/** SOS workflow states for the confirmation flow. */
enum class SosState { READY, CONFIRMING, ACTIVE, ACKNOWLEDGED, RESOLVED }

// ─────────────────────────────────────────────────────────────────────────────
// Repository Interface
// ─────────────────────────────────────────────────────────────────────────────

interface EmergencyRepository {
    fun getEmergencyEvents(): Flow<List<EmergencyEvent>>
    suspend fun createEmergencyEvent(event: EmergencyEvent)
    suspend fun resolveEmergencyEvent(id: String)
    suspend fun acknowledgeEmergencyEvent(id: String)
}

// ─────────────────────────────────────────────────────────────────────────────
// Mappers
// ─────────────────────────────────────────────────────────────────────────────

fun EmergencyEventEntity.toDomain() = EmergencyEvent(
    id = entityId,
    type = emergencyType,
    priority = priority,
    status = status,
    senderId = senderId,
    message = message,
    latitude = latitude,
    longitude = longitude,
    timestamp = timestamp,
    isResolved = isResolved,
    ttl = ttl
)

fun EmergencyEvent.toEntity() = EmergencyEventEntity(
    entityId = id,
    emergencyType = type,
    priority = priority,
    status = status,
    senderId = senderId,
    message = message,
    latitude = latitude,
    longitude = longitude,
    timestamp = timestamp,
    isResolved = isResolved,
    ttl = ttl
)
