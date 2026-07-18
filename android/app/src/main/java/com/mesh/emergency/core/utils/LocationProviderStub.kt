/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.utils

import com.mesh.emergency.core.common.result.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Temporary location provider stub used to build core architecture compilation targets.
 */
@Singleton
class LocationProviderStub @Inject constructor() : LocationProvider {

    override fun getCurrentLocation(): Flow<Result<LocationData>> = flow {
        emit(
            Result.Success(
                LocationData(
                    id = UUID.randomUUID().toString(),
                    latitude = 0.0,
                    longitude = 0.0,
                    altitude = 0.0,
                    accuracy = 0.0f,
                    timestamp = System.currentTimeMillis(),
                    provider = "mock",
                    deviceId = "mock_device_id"
                )
            )
        )
    }

    override fun getLastKnownLocation(): Flow<Result<LocationData>> = flow {
        emit(
            Result.Success(
                LocationData(
                    id = UUID.randomUUID().toString(),
                    latitude = 0.0,
                    longitude = 0.0,
                    altitude = 0.0,
                    accuracy = 0.0f,
                    timestamp = System.currentTimeMillis(),
                    provider = "mock",
                    deviceId = "mock_device_id"
                )
            )
        )
    }

    override fun startTracking() {
        // Stub implementation
    }

    override fun stopTracking() {
        // Stub implementation
    }

    override fun checkAvailability(): LocationState = LocationState.AVAILABLE
}
