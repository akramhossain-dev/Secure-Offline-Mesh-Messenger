/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.identity

/**
 * Interface contract for computing device recognition fingerprints.
 */
interface DeviceFingerprintProvider {
    /**
     * Returns a non-sensitive device fingerprint string based on hardware details.
     */
    fun getDeviceFingerprint(): String
}
