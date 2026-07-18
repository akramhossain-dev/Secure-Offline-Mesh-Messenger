/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.identity

/**
 * Interface contract for offline, collision-resistant identifier generation.
 */
interface IdentityGenerator {
    /** Generates a unique user identifier. */
    fun generateUserId(): String

    /** Generates a unique device identifier. */
    fun generateDeviceId(): String

    /** Generates a unique network node identifier. */
    fun generateNodeId(): String

    /** Generates a random cryptographic fingerprint seed. */
    fun generateFingerprint(): String
}
