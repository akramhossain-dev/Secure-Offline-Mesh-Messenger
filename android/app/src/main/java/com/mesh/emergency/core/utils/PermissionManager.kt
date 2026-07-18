/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.utils

import android.content.Context

/**
 * Interface contract for managing Android runtime permissions.
 */
interface PermissionManager {
    /**
     * Checks if a single [permission] has been granted.
     */
    fun hasPermission(context: Context, permission: String): Boolean

    /**
     * Checks if all listed [permissions] have been granted.
     */
    fun hasPermissions(context: Context, vararg permissions: String): Boolean

    /**
     * Returns true if the app should show UI explaining why the permission is needed.
     */
    fun shouldShowRationale(activity: android.app.Activity, permission: String): Boolean
}
