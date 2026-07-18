/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.utils.capability

import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [DeviceCapabilityManager] auditing local system features.
 */
@Singleton
class DeviceCapabilityManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : DeviceCapabilityManager {

    override fun isBluetoothSupported(): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) ||
                context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)
    }

    override fun isLocationSupported(): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS)
    }

    override fun isMicrophoneSupported(): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)
    }

    override fun isNotificationsSupported(): Boolean {
        // Platform notification support is default
        return true
    }

    override fun isGpsEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        return locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true
    }
}
