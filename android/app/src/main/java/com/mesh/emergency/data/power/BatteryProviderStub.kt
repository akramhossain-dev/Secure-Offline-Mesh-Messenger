/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.power

import com.mesh.emergency.core.power.BatteryModel
import com.mesh.emergency.core.power.BatteryProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation stub of [BatteryProvider] returning mock levels for testing.
 */
@Singleton
class BatteryProviderStub @Inject constructor() : BatteryProvider {

    override fun getBatteryStatus(): Flow<BatteryModel> = flow {
        emit(
            BatteryModel(
                level = 80,
                isCharging = false,
                powerSource = "battery",
                health = "good",
                temperature = 28.5f,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    override fun isCharging(): Boolean = false

    override fun getBatteryLevel(): Int = 80
}
