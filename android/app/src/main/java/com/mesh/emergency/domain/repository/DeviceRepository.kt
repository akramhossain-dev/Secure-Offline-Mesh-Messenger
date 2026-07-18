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

    /** Updates trust status values on device identifiers. */
    suspend fun updateTrustStatus(deviceId: String, status: String): Result<Unit>

    /** Pairs a device by caching details and status in local database. */
    suspend fun pairDevice(deviceId: String, name: String, deviceType: String, platformInfo: String): Result<Unit>
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
