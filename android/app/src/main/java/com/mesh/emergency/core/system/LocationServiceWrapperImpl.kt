/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.system

import android.content.Context
import android.location.LocationManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [LocationServiceWrapper] wrapping system [LocationManager].
 */
@Singleton
class LocationServiceWrapperImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : LocationServiceWrapper {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    override fun isLocationProviderEnabled(provider: String): Boolean {
        return locationManager?.isProviderEnabled(provider) == true
    }

    override fun requestLocationUpdates() {
        // Platform location update wrapper placeholder.
    }
}
