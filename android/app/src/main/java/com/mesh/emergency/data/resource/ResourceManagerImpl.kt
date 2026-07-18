/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.resource

import com.mesh.emergency.core.common.result.Result
import com.mesh.emergency.core.communication.CommunicationManager
import com.mesh.emergency.core.identity.DeviceFingerprintProvider
import com.mesh.emergency.core.resource.ResourceManager
import com.mesh.emergency.data.local.LocalDataSource
import com.mesh.emergency.data.local.entity.DbResourcePrivacy
import com.mesh.emergency.data.local.entity.DbResourceStatus
import com.mesh.emergency.data.local.entity.ResourceEntity
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [ResourceManager] persisting resource packets in local Room database.
 */
@Singleton
class ResourceManagerImpl @Inject constructor(
    private val localDataSource: LocalDataSource,
    private val communicationManager: CommunicationManager,
    private val deviceFingerprintProvider: DeviceFingerprintProvider
) : ResourceManager {

    override suspend fun createOffer(
        name: String,
        type: String,
        quantity: Int,
        latitude: Double,
        longitude: Double,
        description: String,
        privacy: String
    ): Result<ResourceEntity> {
        return try {
            val now = System.currentTimeMillis()
            val userId = deviceFingerprintProvider.getDeviceFingerprint()
            val offer = ResourceEntity(
                entityId = UUID.randomUUID().toString(),
                ownerId = userId,
                name = name,
                type = type,
                quantity = quantity,
                latitude = latitude,
                longitude = longitude,
                description = description,
                availabilityStatus = DbResourceStatus.AVAILABLE,
                createdTime = now,
                updatedTime = now,
                privacyLevel = DbResourcePrivacy.valueOf(privacy),
                ttl = now + 86400000L * 3 // Offers have 3 days default TTL
            )
            localDataSource.insertResource(offer)

            val payload = "[RESOURCE_OFFER] Name:$name Qty:$quantity".toByteArray()
            communicationManager.sendMessage(payload)

            Result.Success(offer)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun createRequest(
        type: String,
        requiredQuantity: Int,
        priority: String
    ): Result<ResourceEntity> {
        return try {
            val now = System.currentTimeMillis()
            val userId = deviceFingerprintProvider.getDeviceFingerprint()
            val request = ResourceEntity(
                entityId = UUID.randomUUID().toString(),
                ownerId = userId,
                name = "Request for $type",
                type = type,
                quantity = requiredQuantity,
                latitude = 0.0,
                longitude = 0.0,
                description = "Emergency Request. Priority: $priority",
                availabilityStatus = DbResourceStatus.LIMITED,
                createdTime = now,
                updatedTime = now,
                privacyLevel = DbResourcePrivacy.PUBLIC,
                ttl = now + 86400000L // Requests have 1 day default TTL
            )
            localDataSource.insertResource(request)

            val payload = "[RESOURCE_REQUEST] Type:$type Qty:$requiredQuantity".toByteArray()
            communicationManager.sendMessage(payload)

            Result.Success(request)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun matchRequestToOffer(requestId: String): Result<List<ResourceEntity>> {
        return try {
            val list = localDataSource.getResources().first()
            val request = list.firstOrNull { it.entityId == requestId }
                ?: return Result.Error(Exception("Request not found"))

            val matches = list.filter {
                it.entityId != requestId &&
                it.type.equals(request.type, ignoreCase = true) &&
                (it.availabilityStatus == DbResourceStatus.AVAILABLE || it.availabilityStatus == DbResourceStatus.LIMITED)
            }
            Result.Success(matches)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun updateAvailability(resourceId: String, status: String): Result<Unit> {
        return try {
            val list = localDataSource.getResources().first()
            val match = list.firstOrNull { it.entityId == resourceId }
            if (match != null) {
                val updated = match.copy(
                    availabilityStatus = DbResourceStatus.valueOf(status),
                    updatedTime = System.currentTimeMillis()
                )
                localDataSource.insertResource(updated)
                Result.Success(Unit)
            } else {
                Result.Error(Exception("Resource not found"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun expireResources(): Result<Unit> {
        return try {
            val list = localDataSource.getResources().first()
            val now = System.currentTimeMillis()
            list.forEach { entity ->
                if (entity.ttl < now && entity.availabilityStatus != DbResourceStatus.EXPIRED) {
                    val expired = entity.copy(availabilityStatus = DbResourceStatus.EXPIRED)
                    localDataSource.insertResource(expired)
                }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
