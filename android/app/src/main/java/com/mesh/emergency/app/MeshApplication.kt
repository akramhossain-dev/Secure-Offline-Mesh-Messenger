/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.app

import android.app.Application
import com.mesh.emergency.BuildConfig
import com.mesh.emergency.core.common.logging.AppLogger
import com.mesh.emergency.core.system.MeshWorkManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

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

    @Inject
    lateinit var meshWorkManager: MeshWorkManager

    override fun onCreate() {
        super.onCreate()
        initializeLogging()
        // Initialize global CrashHandler with application context for local crash log persistence
        com.mesh.emergency.core.error.CrashHandler.install(this)
        
        // Schedule all periodic background sync, routing, and cleanup workers
        meshWorkManager.scheduleAllWorkers()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private initialization helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Initializes Timber logging.
     * - Debug builds: DebugTree with full stack traces
     * - Release builds: ReleaseTree writing ERROR+ to filesDir/logs/error.log
     */
    private fun initializeLogging() {
        AppLogger.initialize(isDebug = BuildConfig.DEBUG, context = this)
    }
}
