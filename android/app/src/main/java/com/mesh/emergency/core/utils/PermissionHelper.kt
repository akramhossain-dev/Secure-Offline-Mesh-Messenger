/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Permission helper utilities for the Offline Emergency Mesh Communication System.
 *
 * This object provides:
 * - Permission group definitions (grouped by feature)
 * - Runtime permission check helpers
 * - API-level-aware permission lists
 *
 * Phase A1: Defines permission groups for all features.
 * Phase A2+: Feature modules will use these groups to request permissions
 *            at the appropriate time, following the Privacy by Default principle.
 *
 * @see docs/app/permission-privacy.md for the full permission matrix.
 */
object PermissionHelper {

    // ─────────────────────────────────────────────────────────────────────────
    // Permission Groups by Feature
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Bluetooth permissions required for BLE communication.
     * API 31+ uses new BLUETOOTH_SCAN/CONNECT permissions.
     * API < 31 uses legacy BLUETOOTH/ACCESS_FINE_LOCATION.
     */
    val bluetoothPermissions: List<String>
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE,
            )
        } else {
            listOf(
                @Suppress("DEPRECATION")
                Manifest.permission.BLUETOOTH,
                @Suppress("DEPRECATION")
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION,
            )
        }

    /**
     * Location permissions required for GPS coordinate capture.
     * Emergency SOS and Location Share features require ACCESS_FINE_LOCATION.
     */
    val locationPermissions: List<String> = listOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )

    /**
     * Camera permission required for QR code scanning.
     */
    val cameraPermissions: List<String> = listOf(
        Manifest.permission.CAMERA,
    )

    /**
     * Microphone permission required for voice message recording.
     */
    val microphonePermissions: List<String> = listOf(
        Manifest.permission.RECORD_AUDIO,
    )

    /**
     * Notification permission (Android 13+).
     */
    val notificationPermissions: List<String>
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            emptyList()
        }

    // ─────────────────────────────────────────────────────────────────────────
    // Check helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns `true` if the given [permission] is granted in [context].
     */
    fun isGranted(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Returns `true` if ALL of the given [permissions] are granted in [context].
     */
    fun areAllGranted(context: Context, permissions: List<String>): Boolean =
        permissions.all { isGranted(context, it) }

    /**
     * Returns the list of [permissions] that are NOT yet granted in [context].
     */
    fun getDenied(context: Context, permissions: List<String>): List<String> =
        permissions.filterNot { isGranted(context, it) }

    /**
     * Returns `true` if all Bluetooth permissions are granted.
     */
    fun hasBluetoothPermissions(context: Context): Boolean =
        areAllGranted(context, bluetoothPermissions)

    /**
     * Returns `true` if all Location permissions are granted.
     */
    fun hasLocationPermissions(context: Context): Boolean =
        areAllGranted(context, locationPermissions)

    /**
     * Returns `true` if Camera permission is granted.
     */
    fun hasCameraPermission(context: Context): Boolean =
        isGranted(context, Manifest.permission.CAMERA)

    /**
     * Returns `true` if Microphone permission is granted.
     */
    fun hasMicrophonePermission(context: Context): Boolean =
        isGranted(context, Manifest.permission.RECORD_AUDIO)
}
