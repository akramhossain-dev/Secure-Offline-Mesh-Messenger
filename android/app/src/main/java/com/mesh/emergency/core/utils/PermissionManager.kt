/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.utils

import android.content.Context
import com.mesh.emergency.core.utils.permission.PermissionState
import com.mesh.emergency.core.utils.permission.PermissionType

/**
 * Interface contract for managing Android runtime permissions using typed categories.
 */
interface PermissionManager {

    /**
     * Inspects permission states for a specific [type].
     */
    fun getPermissionState(context: Context, type: PermissionType): PermissionState

    /**
     * Checks if all required permissions for a [type] have been granted.
     */
    fun hasPermission(context: Context, type: PermissionType): Boolean

    /**
     * Returns true if the app should show UI explaining why the permissions are needed.
     */
    fun shouldShowRationale(activity: android.app.Activity, type: PermissionType): Boolean
}
