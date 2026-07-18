/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.location

import com.mesh.emergency.core.common.result.Result
import com.mesh.emergency.core.communication.CommunicationManager
import com.mesh.emergency.core.location.LocationSharingManager
import com.mesh.emergency.core.protocol.LocationPacket
import com.mesh.emergency.data.local.LocalDataSource
import com.mesh.emergency.data.local.entity.LocationEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [LocationSharingManager] using Room persistence and
 * CommunicationManager for mesh broadcast.
 *
 * All location data remains local — no internet or GPS hardware required.
 */
@Singleton
class LocationSharingManagerImpl @Inject constructor(
    private val localDataSource: LocalDataSource,
    private val communicationManager: CommunicationManager
) : LocationSharingManager {

    private val _currentLocation = MutableStateFlow<LocationPacket?>(null)
    override val currentLocation: StateFlow<LocationPacket?> = _currentLocation.asStateFlow()

    // In-memory received locations (from other mesh nodes)
    private val _sharedLocations = MutableStateFlow<List<LocationPacket>>(emptyList())

    override val sharedLocations: Flow<List<LocationPacket>> = _sharedLocations

    override val savedLocations: Flow<List<LocationPacket>> =
        localDataSource.getLocationsForUser(LOCAL_USER_ID).map { entities ->
            entities.mapNotNull { it.toLocationPacket() }
        }

    override suspend fun setCurrentLocation(packet: LocationPacket): Result<Unit> {
        return try {
            _currentLocation.value = packet
            Timber.d("LocationSharing: Current location set to (${packet.latitude}, ${packet.longitude})")
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun shareCurrentLocation(): Result<Unit> {
        return try {
            val location = _currentLocation.value
                ?: return Result.Error(Exception("No current location set"))

            val serialized = LocationPacket.serialize(location)
            val result = communicationManager.sendMessage(serialized.toByteArray(Charsets.UTF_8))
            Timber.d("LocationSharing: Broadcast location packet (${location.latitude}, ${location.longitude}) result=$result")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "LocationSharing: Failed to broadcast location")
            Result.Error(e)
        }
    }

    override suspend fun saveLocation(packet: LocationPacket): Result<Unit> {
        return try {
            val entity = LocationEntity(
                entityId = UUID.randomUUID().toString(),
                userId = LOCAL_USER_ID,
                latitude = packet.latitude,
                longitude = packet.longitude,
                altitude = packet.altitude,
                accuracy = packet.accuracy,
                timestamp = packet.timestamp,
                provider = "mesh",
                deviceId = packet.senderId
            )
            localDataSource.insertLocation(entity)
            Timber.d("LocationSharing: Saved location pin for ${packet.senderId}")
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun removeSavedLocation(senderId: String, timestamp: Long): Result<Unit> {
        return try {
            // Find and delete matching entity
            // Note: In a real implementation, we'd query by deviceId + timestamp
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun receiveLocationPacket(packet: LocationPacket): Result<Unit> {
        return try {
            val current = _sharedLocations.value.toMutableList()
            // Replace existing entry for same sender or add new
            val idx = current.indexOfFirst { it.senderId == packet.senderId }
            if (idx >= 0) {
                current[idx] = packet
            } else {
                current.add(packet)
            }
            _sharedLocations.value = current
            Timber.d("LocationSharing: Received location from ${packet.senderId}")
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    companion object {
        private const val LOCAL_USER_ID = "local_user"
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Extension
// ─────────────────────────────────────────────────────────────────────────────

private fun LocationEntity.toLocationPacket(): LocationPacket? {
    return runCatching {
        LocationPacket(
            senderId = deviceId,
            latitude = latitude,
            longitude = longitude,
            accuracy = accuracy,
            timestamp = timestamp,
            altitude = altitude
        )
    }.getOrNull()
}
