/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.utils

import com.mesh.emergency.core.common.result.Result
import kotlinx.coroutines.flow.Flow

/**
 * Interface contract for receiving location updates.
 */
interface LocationProvider {
    /**
     * Streams the current location of the device.
     */
    fun getCurrentLocation(): Flow<Result<LocationData>>

    /**
     * Streams the last cached location of the device.
     */
    fun getLastKnownLocation(): Flow<Result<LocationData>>
}

/**
 * Pure data representation of GPS location coordinates.
 */
data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val timestamp: Long
)
