/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.overlay

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Isolated permission manager checking and requesting SYSTEM_ALERT_WINDOW (overlay) permission.
 */
@Singleton
class OverlayPermission @Inject constructor() {

    /** Checks whether the app has SYSTEM_ALERT_WINDOW overlay permission. */
    fun canDrawOverlays(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    /** Creates an Intent pointing to system settings to grant overlay permission. */
    fun createPermissionIntent(context: Context): Intent {
        return Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
    }
}
