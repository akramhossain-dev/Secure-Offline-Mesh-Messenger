/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.utils

/**
 * Android GPS provider capability and permission states enums.
 */
enum class LocationState {
    AVAILABLE,
    SEARCHING,
    DISABLED,
    PERMISSION_MISSING,
    UNAVAILABLE,
    ERROR
}
