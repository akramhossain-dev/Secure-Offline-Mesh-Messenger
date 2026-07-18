/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.common.logging

import android.content.Context
import android.util.Log
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Centralized logging abstraction for the Offline Emergency Mesh Communication System.
 *
 * Wraps Timber to provide:
 * - Structured log levels (DEBUG, INFO, WARN, ERROR)
 * - Module-tagged logging for easy filtering in Logcat
 * - Release-safe: DEBUG/INFO/WARN logs are no-ops in release builds
 * - ERROR logs are written to [Context.filesDir]/logs/error.log in release builds
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
     *                `false` in release builds — plants a file-backed [ReleaseTree].
     * @param context Application context required for file-backed error logging in release.
     */
    fun initialize(isDebug: Boolean, context: Context? = null) {
        if (isDebug) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ReleaseTree(context))
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
    // Release Tree — persists ERROR+ to local file, discards lower priority
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Production Timber tree that:
     * - Silently discards VERBOSE, DEBUG, INFO, WARN logs (no Logcat leakage).
     * - Writes ERROR and WTF entries to [Context.filesDir]/logs/error.log (200 KB cap).
     */
    private class ReleaseTree(private val context: Context?) : Timber.Tree() {

        private val logFile: File? by lazy {
            context?.let {
                File(it.filesDir, "logs/error.log").also { f -> f.parentFile?.mkdirs() }
            }
        }

        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            if (priority < Log.ERROR) return

            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            val level = if (priority == Log.ASSERT) "WTF" else "ERROR"
            val entry = buildString {
                appendLine("[$timestamp] $level/$tag: $message")
                t?.let { appendLine("  Exception: ${it::class.simpleName}: ${it.message}") }
            }

            try {
                val file = logFile ?: return
                // Rotate at 200 KB
                if (file.exists() && file.length() > 200_000L) {
                    val text = file.readText()
                    file.writeText(text.drop(text.length / 2))
                }
                file.appendText(entry)
            } catch (_: Exception) { /* Cannot log the logger failing */ }
        }
    }
}
