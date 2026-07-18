/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.discovery

import com.mesh.emergency.domain.repository.DeviceDomainModel
import kotlinx.coroutines.flow.Flow

/**
 * Interface contract representing transport-independent device discovery provider methods.
 */
interface DiscoveryProvider {
    /** Starts discovering nearby candidates nodes. */
    fun startDiscovery()

    /** Stops discovering nearby candidates nodes. */
    fun stopDiscovery()

    /** Streams lists of discovered device domain models. */
    fun discoverDevices(): Flow<List<DeviceDomainModel>>
}
