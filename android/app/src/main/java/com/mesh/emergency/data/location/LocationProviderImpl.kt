/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import com.mesh.emergency.core.common.result.Result
import com.mesh.emergency.core.utils.LocationData
import com.mesh.emergency.core.utils.LocationProvider
import com.mesh.emergency.core.utils.LocationState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real implementation of [LocationProvider] using Android's [LocationManager] to deliver
 * GPS and network fused location updates.
 *
 * Replaces [LocationProviderStub] in production DI binding.
 * Requires [android.Manifest.permission.ACCESS_FINE_LOCATION] at runtime.
 */
@Singleton
class LocationProviderImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : LocationProvider {

    private val locationManager: LocationManager? by lazy {
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    }

    /** Holds the most recently received location — used for getLastKnownLocation(). */
    private val lastLocation = MutableStateFlow<Location?>(null)

    private val deviceId: String by lazy { UUID.randomUUID().toString() }

    @SuppressLint("MissingPermission")
    override fun getCurrentLocation(): Flow<Result<LocationData>> = callbackFlow {
        val lm = locationManager
        if (lm == null) {
            trySend(Result.Error(Exception("LocationManager unavailable")))
            close()
            return@callbackFlow
        }

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                lastLocation.value = location
                trySend(Result.Success(location.toLocationData()))
                Timber.d("LocationProvider: lat=${location.latitude} lng=${location.longitude} provider=${location.provider}")
            }

            @Deprecated("Deprecated in API 29")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
            override fun onProviderEnabled(provider: String) = Unit
            override fun onProviderDisabled(provider: String) {
                Timber.w("LocationProvider: provider disabled — $provider")
            }
        }

        val availableProvider = when {
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER)     -> LocationManager.GPS_PROVIDER
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> null
        }

        if (availableProvider == null) {
            trySend(Result.Error(Exception("No location providers available")))
            close()
            return@callbackFlow
        }

        try {
            lm.requestLocationUpdates(availableProvider, MIN_TIME_MS, MIN_DISTANCE_M, listener)
        } catch (e: Exception) {
            Timber.e(e, "LocationProvider: requestLocationUpdates failed")
            trySend(Result.Error(e))
            close()
            return@callbackFlow
        }

        awaitClose {
            try { lm.removeUpdates(listener) } catch (_: Exception) {}
        }
    }

    @SuppressLint("MissingPermission")
    override fun getLastKnownLocation(): Flow<Result<LocationData>> {
        // Attempt to seed the lastLocation from system if not yet populated
        if (lastLocation.value == null) {
            val lm = locationManager
            val cached = lm?.let {
                runCatching { it.getLastKnownLocation(LocationManager.GPS_PROVIDER) }.getOrNull()
                    ?: runCatching { it.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) }.getOrNull()
            }
            if (cached != null) lastLocation.value = cached
        }

        return lastLocation
            .filterNotNull()
            .map { Result.Success(it.toLocationData()) as Result<LocationData> }
    }

    override fun startTracking() {
        // Tracking is driven by Flow collection in getCurrentLocation().
        // This method exists for imperative callers; no-op since flows handle lifecycle.
        Timber.d("LocationProvider: startTracking() called — collect getCurrentLocation() flow to receive updates")
    }

    override fun stopTracking() {
        Timber.d("LocationProvider: stopTracking() called — cancel getCurrentLocation() flow collection")
    }

    override fun checkAvailability(): LocationState {
        val lm = locationManager ?: return LocationState.UNAVAILABLE
        return when {
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER)     -> LocationState.AVAILABLE
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationState.AVAILABLE
            else                                                    -> LocationState.UNAVAILABLE
        }
    }

    private fun Location.toLocationData() = LocationData(
        id = UUID.randomUUID().toString(),
        latitude = latitude,
        longitude = longitude,
        altitude = altitude,
        accuracy = accuracy,
        timestamp = time,
        provider = provider ?: "unknown",
        deviceId = deviceId
    )

    companion object {
        private const val MIN_TIME_MS = 5_000L      // 5 seconds between updates
        private const val MIN_DISTANCE_M = 5.0f     // 5 metres minimum movement
    }
}
