/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.emergency

import com.mesh.emergency.core.common.result.Result
import com.mesh.emergency.data.local.entity.EmergencyEventEntity
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface contract coordinating emergency SOS modes and lifecycle sweeps.
 */
interface EmergencyManager {
    /** Exposes active emergency mode status. */
    val isEmergencyMode: StateFlow<Boolean>

    /** Triggers a critical SOS distress beacon broadcast. */
    suspend fun triggerSOS(latitude: Double, longitude: Double, message: String): Result<EmergencyEventEntity>

    /** Acknowledges receipt of remote SOS beacon. */
    suspend fun acknowledgeSOS(emergencyId: String, responseType: String): Result<Unit>

    /** Resolves an active emergency. */
    suspend fun resolveEmergency(emergencyId: String): Result<Unit>

    /** Cancels an active emergency. */
    suspend fun cancelEmergency(emergencyId: String): Result<Unit>
}
