/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.utils.permission

/**
 * Enumeration returned by request handlers callbacks.
 */
enum class PermissionResult {
    GRANTED,
    DENIED,
    PERMANENTLY_DENIED
}
