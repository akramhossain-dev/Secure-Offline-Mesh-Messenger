/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.security

/**
 * Security status states representing verification checkpoints.
 */
enum class SecurityState {
    SECURE,
    UNVERIFIED,
    KEY_MISSING,
    AUTHENTICATION_FAILED
}
