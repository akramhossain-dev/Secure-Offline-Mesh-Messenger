/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.utils.permission

/**
 * sealed hierarchy representing permission states in the application logic.
 */
sealed interface PermissionState {
    /** Active and fully granted. */
    data object Granted : PermissionState

    /** Denied on last request dialog. */
    data object Denied : PermissionState

    /** Denied permanently; must open settings panel to grant. */
    data object PermanentlyDenied : PermissionState

    /** Has never been requested yet. */
    data object NotRequested : PermissionState
}
