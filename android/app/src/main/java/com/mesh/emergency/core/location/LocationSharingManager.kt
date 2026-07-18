/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.location

import com.mesh.emergency.core.common.result.Result
import com.mesh.emergency.core.protocol.LocationPacket
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Manager interface coordinating location sharing in offline mesh context.
 *
 * All location data is local — no GPS hardware access. Locations are:
 * 1. Saved manually by the user
 * 2. Received from other mesh nodes via [LocationPacket]
 */
interface LocationSharingManager {

    /** Current simulated device location (null until set). */
    val currentLocation: StateFlow<LocationPacket?>

    /** Stream of all shared locations from other nodes. */
    val sharedLocations: Flow<List<LocationPacket>>

    /** Stream of saved (pinned) locations. */
    val savedLocations: Flow<List<LocationPacket>>

    /** Sets the current device location for broadcasting. */
    suspend fun setCurrentLocation(packet: LocationPacket): Result<Unit>

    /** Broadcasts current location to mesh network. */
    suspend fun shareCurrentLocation(): Result<Unit>

    /** Saves a location as a persistent pin. */
    suspend fun saveLocation(packet: LocationPacket): Result<Unit>

    /** Removes a saved location pin. */
    suspend fun removeSavedLocation(senderId: String, timestamp: Long): Result<Unit>

    /** Processes an incoming location packet received from the mesh. */
    suspend fun receiveLocationPacket(packet: LocationPacket): Result<Unit>
}
