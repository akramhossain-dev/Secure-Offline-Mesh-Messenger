/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.system

/**
 * Interface contract wrapping Android LocationManager calls.
 */
interface LocationServiceWrapper {
    /** Checks if a specific coordinates provider (e.g. GPS, Network) is active. */
    fun isLocationProviderEnabled(provider: String): Boolean

    /** Stub requesting coordinates sync. */
    fun requestLocationUpdates()
}
