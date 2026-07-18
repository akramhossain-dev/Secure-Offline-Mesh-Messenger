/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mesh.emergency.core.model.BaseEntity

/**
 * Database Entity mapping resources offers and requests logs.
 */
@Entity(tableName = "resources")
data class ResourceEntity(
    @PrimaryKey override val entityId: String,
    val ownerId: String,
    val name: String,
    val type: String,
    val quantity: Int,
    val latitude: Double,
    val longitude: Double,
    val description: String,
    val availabilityStatus: DbResourceStatus = DbResourceStatus.AVAILABLE,
    val createdTime: Long = System.currentTimeMillis(),
    val updatedTime: Long = System.currentTimeMillis(),
    val privacyLevel: DbResourcePrivacy = DbResourcePrivacy.PUBLIC,
    val ttl: Long = System.currentTimeMillis() + 86400000L
) : BaseEntity

/**
 * Resource availability status.
 */
enum class DbResourceStatus {
    AVAILABLE,
    LIMITED,
    RESERVED,
    UNAVAILABLE,
    EXPIRED
}

/**
 * Resource privacy bounds.
 */
enum class DbResourcePrivacy {
    PUBLIC,
    TRUSTED_ONLY,
    PRIVATE
}
