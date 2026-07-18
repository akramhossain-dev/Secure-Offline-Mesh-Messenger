/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.domain.repository

import com.mesh.emergency.core.common.result.Result
import com.mesh.emergency.core.model.BaseModel
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface regulating BLE device scans and device rosters.
 */
interface DeviceRepository {
    /** Stream of active BLE nodes discovered. */
    fun getDiscoveredDevices(): Flow<Result<List<DeviceDomainModel>>>

    /** Directs Bluetooth central to start scanning. */
    suspend fun startScan(): Result<Unit>

    /** Directs Bluetooth central to stop scanning. */
    suspend fun stopScan(): Result<Unit>
}

/**
 * Domain model representing a discovered peripheral node.
 */
data class DeviceDomainModel(
    override val id: String,
    val name: String,
    val rssi: Int,
    val lastSeen: Long
) : BaseModel
