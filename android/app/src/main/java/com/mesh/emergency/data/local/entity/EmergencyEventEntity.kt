/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mesh.emergency.core.model.BaseEntity

/**
 * Database Entity mapping emergency SOS beacons telemetry logs.
 */
@Entity(tableName = "emergency_events")
data class EmergencyEventEntity(
    @PrimaryKey override val entityId: String,
    val senderId: String,
    val latitude: Double,
    val longitude: Double,
    val message: String,
    val timestamp: Long,
    val isResolved: Boolean,
    val emergencyType: DbEmergencyType = DbEmergencyType.SOS,
    val priority: DbMessagePriority = DbMessagePriority.CRITICAL,
    val status: DbEmergencyStatus = DbEmergencyStatus.CREATED,
    val ttl: Long = System.currentTimeMillis() + 86400000L
) : BaseEntity

/**
 * Emergency event types.
 */
enum class DbEmergencyType {
    SOS,
    MEDICAL,
    ACCIDENT,
    DISASTER,
    CUSTOM
}

/**
 * Emergency SOS lifecycle states.
 */
enum class DbEmergencyStatus {
    CREATED,
    SENDING,
    BROADCASTING,
    RECEIVED,
    ACKNOWLEDGED,
    RESOLVED,
    CANCELLED
}
