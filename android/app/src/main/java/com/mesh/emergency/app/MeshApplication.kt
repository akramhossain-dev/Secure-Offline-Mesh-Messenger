/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.app

import android.app.Application
import com.mesh.emergency.BuildConfig
import com.mesh.emergency.core.common.logging.AppLogger
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point for the Offline Emergency Mesh Communication System.
 *
 * Responsibilities:
 * - Initialize Hilt dependency injection graph
 * - Initialize logging (Timber) — debug builds only
 * - Set up global exception handlers (Phase A2+)
 * - Prepare global application state
 *
 * This class must be declared in AndroidManifest.xml via android:name.
 */
@HiltAndroidApp
class MeshApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initializeLogging()
        // Initialize global CrashHandler (A34.9)
        com.mesh.emergency.core.error.CrashHandler.install()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private initialization helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Initializes Timber logging.
     * - Debug builds: DebugTree with full stack traces
     * - Release builds: No-op (logs stripped by ProGuard)
     */
    private fun initializeLogging() {
        AppLogger.initialize(isDebug = BuildConfig.DEBUG)
    }
}
