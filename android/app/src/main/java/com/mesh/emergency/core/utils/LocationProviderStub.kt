/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.utils

import com.mesh.emergency.core.common.result.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Temporary location provider stub used to build core architecture compilation targets.
 */
@Singleton
class LocationProviderStub @Inject constructor() : LocationProvider {

    override fun getCurrentLocation(): Flow<Result<LocationData>> = flow {
        emit(Result.Success(LocationData(0.0, 0.0, 0.0f, System.currentTimeMillis())))
    }

    override fun getLastKnownLocation(): Flow<Result<LocationData>> = flow {
        emit(Result.Success(LocationData(0.0, 0.0, 0.0f, System.currentTimeMillis())))
    }
}
