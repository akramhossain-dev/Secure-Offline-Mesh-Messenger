/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.common.extensions

import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat

// ─────────────────────────────────────────────────────────────────────────────
// Context Extension Functions
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Returns `true` if the given [permission] has been granted to this app.
 */
fun Context.hasPermission(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

/**
 * Returns `true` if ALL of the given [permissions] have been granted.
 */
fun Context.hasPermissions(vararg permissions: String): Boolean =
    permissions.all { hasPermission(it) }

/**
 * Shows a short [Toast] with the given string [message].
 */
fun Context.showToast(message: String) =
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

/**
 * Shows a short [Toast] with a string resource [messageRes].
 */
fun Context.showToast(@StringRes messageRes: Int) =
    Toast.makeText(this, messageRes, Toast.LENGTH_SHORT).show()

/**
 * Shows a long [Toast] with the given string [message].
 */
fun Context.showLongToast(message: String) =
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
