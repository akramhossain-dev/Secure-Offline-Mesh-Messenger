/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.common.logging

import timber.log.Timber

/**
 * Centralized logging abstraction for the Offline Emergency Mesh Communication System.
 *
 * Wraps Timber to provide:
 * - Structured log levels (DEBUG, INFO, WARN, ERROR)
 * - Module-tagged logging for easy filtering in Logcat
 * - Release-safe: all logs are no-ops in release builds
 * - Single initialization point in [MeshApplication]
 *
 * Usage:
 * ```kotlin
 * AppLogger.d("BLE", "Device discovered: $deviceName")
 * AppLogger.e("CRYPTO", "Encryption failed", exception)
 * ```
 */
object AppLogger {

    /**
     * Initializes Timber.
     *
     * @param isDebug `true` in debug builds — plants a [Timber.DebugTree].
     *                `false` in release builds — plants a no-op [ReleaseTree].
     */
    fun initialize(isDebug: Boolean) {
        if (isDebug) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ReleaseTree())
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Logging API
    // ─────────────────────────────────────────────────────────────────────────

    /** Log a verbose message with an optional module [tag]. */
    fun v(tag: String, message: String) = Timber.tag(tag).v(message)

    /** Log a debug message with an optional module [tag]. */
    fun d(tag: String, message: String) = Timber.tag(tag).d(message)

    /** Log an informational message with an optional module [tag]. */
    fun i(tag: String, message: String) = Timber.tag(tag).i(message)

    /** Log a warning message with an optional module [tag]. */
    fun w(tag: String, message: String) = Timber.tag(tag).w(message)

    /** Log an error message with an optional [throwable] and module [tag]. */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Timber.tag(tag).e(throwable, message)
        } else {
            Timber.tag(tag).e(message)
        }
    }

    /** Log a critical/WTF message with an optional [throwable] and module [tag]. */
    fun wtf(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Timber.tag(tag).wtf(throwable, message)
        } else {
            Timber.tag(tag).wtf(message)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Release Tree — silently discards all logs in production
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Production Timber tree that only logs WARN and ERROR levels,
     * and never logs sensitive information.
     *
     * Extend this in Phase A2 to forward critical errors to a local
     * crash log file (no network reporting — offline-first requirement).
     */
    private class ReleaseTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            // In release: only forward ERROR and ASSERT priority
            // Extend this in Phase A2 to write to a local error log file
            if (priority >= android.util.Log.ERROR) {
                // TODO (Phase A2): Write to local crash log file
            }
        }
    }
}
