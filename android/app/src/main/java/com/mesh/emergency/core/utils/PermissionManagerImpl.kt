/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.utils

import android.content.Context
import androidx.core.app.ActivityCompat
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PermissionManager implementation wrapping [PermissionHelper] and platform check/request flows.
 */
@Singleton
class PermissionManagerImpl @Inject constructor() : PermissionManager {

    override fun hasPermission(context: Context, permission: String): Boolean {
        return PermissionHelper.isGranted(context, permission)
    }

    override fun hasPermissions(context: Context, vararg permissions: String): Boolean {
        return PermissionHelper.areAllGranted(context, permissions.toList())
    }

    override fun shouldShowRationale(activity: android.app.Activity, permission: String): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }
}
