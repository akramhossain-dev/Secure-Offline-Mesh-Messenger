/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.utils

import android.content.Context
import androidx.core.app.ActivityCompat
import com.mesh.emergency.core.utils.permission.PermissionState
import com.mesh.emergency.core.utils.permission.PermissionType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [PermissionManager] evaluating runtime permissions and rationales.
 */
@Singleton
class PermissionManagerImpl @Inject constructor() : PermissionManager {

    override fun getPermissionState(context: Context, type: PermissionType): PermissionState {
        val permissions = type.getRequiredPermissions()
        if (permissions.isEmpty()) return PermissionState.Granted

        val allGranted = PermissionHelper.areAllGranted(context, permissions)
        if (allGranted) return PermissionState.Granted

        // Evaluates rationale checks to separate standard Denied from PermanentlyDenied states.
        // Needs an Activity reference for rationales; fallback to simple Denied if called from Context.
        val activity = context as? android.app.Activity
        return if (activity != null) {
            val shouldShowAnyRationale = permissions.any { permission ->
                ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
            }
            if (shouldShowAnyRationale) {
                PermissionState.Denied
            } else {
                // If it's not granted and we shouldn't show a rationale, it is either permanently
                // denied (user clicked "don't ask again") or has never been requested.
                // We default to NotRequested unless we can confirm it was requested in user preferences.
                PermissionState.NotRequested
            }
        } else {
            PermissionState.Denied
        }
    }

    override fun hasPermission(context: Context, type: PermissionType): Boolean {
        val permissions = type.getRequiredPermissions()
        return permissions.isEmpty() || PermissionHelper.areAllGranted(context, permissions)
    }

    override fun shouldShowRationale(activity: android.app.Activity, type: PermissionType): Boolean {
        val permissions = type.getRequiredPermissions()
        return permissions.any { permission ->
            ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        }
    }
}
