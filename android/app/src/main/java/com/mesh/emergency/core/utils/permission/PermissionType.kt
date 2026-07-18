/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.utils.permission

import android.Manifest
import android.os.Build

/**
 * Enumerated list of platform features requesting dynamic runtime permissions.
 */
enum class PermissionType {
    /** Bluetooth scan, connect, advertise permissions. */
    BLUETOOTH,

    /** fine and coarse coordinates location permissions. */
    LOCATION,

    /** Audio recording permission. */
    MICROPHONE,

    /** Push alerts notification permission. */
    NOTIFICATION;

    /**
     * Returns required android permission strings matching API level.
     */
    fun getRequiredPermissions(): List<String> {
        return when (this) {
            BLUETOOTH -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    listOf(
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.BLUETOOTH_ADVERTISE
                    )
                } else {
                    listOf(
                        Manifest.permission.BLUETOOTH,
                        Manifest.permission.BLUETOOTH_ADMIN
                    )
                }
            }
            LOCATION -> {
                listOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            }
            MICROPHONE -> {
                listOf(Manifest.permission.RECORD_AUDIO)
            }
            NOTIFICATION -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    listOf(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    emptyList()
                }
            }
        }
    }
}
