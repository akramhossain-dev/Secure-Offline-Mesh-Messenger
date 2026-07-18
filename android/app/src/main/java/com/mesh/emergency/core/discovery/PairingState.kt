/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.discovery

/**
 * Pairing process flow checkpoint states.
 */
enum class PairingState {
    SEARCHING,
    FOUND,
    PAIRING,
    VERIFIED,
    CONNECTED,
    FAILED
}
