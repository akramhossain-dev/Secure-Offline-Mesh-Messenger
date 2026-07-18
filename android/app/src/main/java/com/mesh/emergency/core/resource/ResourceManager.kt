/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.resource

import com.mesh.emergency.core.common.result.Result
import com.mesh.emergency.data.local.entity.ResourceEntity

/**
 * Interface contract coordinating resources requests/offers and categories matching.
 */
interface ResourceManager {
    /** Registers an available emergency resource offer. */
    suspend fun createOffer(
        name: String,
        type: String,
        quantity: Int,
        latitude: Double,
        longitude: Double,
        description: String,
        privacy: String
    ): Result<ResourceEntity>

    /** Registers a resource request. */
    suspend fun createRequest(
        type: String,
        requiredQuantity: Int,
        priority: String
    ): Result<ResourceEntity>

    /** Returns list of available resource offers matching a request. */
    suspend fun matchRequestToOffer(requestId: String): Result<List<ResourceEntity>>

    /** Updates availability status parameters. */
    suspend fun updateAvailability(resourceId: String, status: String): Result<Unit>

    /** Expire resources whose TTL has passed. */
    suspend fun expireResources(): Result<Unit>
}
