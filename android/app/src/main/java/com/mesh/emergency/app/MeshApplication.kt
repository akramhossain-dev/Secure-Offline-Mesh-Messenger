/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.mesh.emergency.BuildConfig
import com.mesh.emergency.core.common.logging.AppLogger
import com.mesh.emergency.core.system.MeshWorkManager
import com.mesh.emergency.data.communication.CommunicationManagerImpl
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Application entry point for the Offline Emergency Mesh Communication System.
 *
 * Responsibilities:
 * - Initialize Hilt dependency injection graph
 * - Initialize logging (Timber) — debug builds only
 * - Set up global exception handlers
 * - Prepare global application state
 * - **Eagerly start BLE transport** so both phones advertise and scan from app launch
 *   regardless of which screen is active. Without eager initialization, the phone showing
 *   the QR code (Profile screen) never starts advertising — making it invisible to the
 *   scanner's BLE reconnect scan after QR pairing.
 *
 * This class must be declared in AndroidManifest.xml via android:name.
 */
@HiltAndroidApp
class MeshApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var meshWorkManager: MeshWorkManager

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    /**
     * Eagerly injected so [CommunicationManagerImpl.init] runs at app startup,
     * kicking off the persistent transport connect() retry loop on every phone.
     * The [CommunicationManagerImpl] is responsible for starting all registered
     * transports — including Bluetooth — via the generic reconnect loop.
     */
    @Inject
    lateinit var communicationManager: CommunicationManagerImpl

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    private val appScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        initializeLogging()
        
        // Track activities to know if the app is in foreground
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: android.app.Activity, savedInstanceState: android.os.Bundle?) {}
            override fun onActivityStarted(activity: android.app.Activity) {
                AppLifecycleTracker.activityStarted()
            }
            override fun onActivityResumed(activity: android.app.Activity) {}
            override fun onActivityPaused(activity: android.app.Activity) {}
            override fun onActivityStopped(activity: android.app.Activity) {
                AppLifecycleTracker.activityStopped()
            }
            override fun onActivitySaveInstanceState(activity: android.app.Activity, outState: android.os.Bundle) {}
            override fun onActivityDestroyed(activity: android.app.Activity) {}
        })

        // Initialize global CrashHandler with application context for local crash log persistence
        com.mesh.emergency.core.error.CrashHandler.install(this)

        // Schedule all periodic background sync, routing, and cleanup workers
        meshWorkManager.scheduleAllWorkers()

        // Start MeshForegroundService to keep communication active in the background
        try {
            val serviceIntent = android.content.Intent(this, com.mesh.emergency.core.system.MeshForegroundService::class.java)
            androidx.core.content.ContextCompat.startForegroundService(this, serviceIntent)
            Timber.d("SERVICE STARTED — MeshApplication starting MeshForegroundService")
        } catch (e: Exception) {
            Timber.e(e, "SERVICE STARTED — Failed to start MeshForegroundService from Application")
        }

        // Eagerly start all transports via CommunicationManager.
        // The Bluetooth transport will start its GATT server and advertising
        // immediately, ensuring this device is discoverable from the first second.
        appScope.launch {
            try {
                Timber.d("COMM_FLOW: MeshApplication eagerly starting all registered transports")
                communicationManager.getTransports().forEach { transport ->
                    transport.connect()
                }
            } catch (e: Exception) {
                Timber.e(e, "COMM_FLOW: Eager transport connect() failed in Application.onCreate()")
            }
        }
    }

    object AppLifecycleTracker {
        private var activeActivities = 0
        val isAppInForeground: Boolean
            get() = activeActivities > 0

        fun activityStarted() {
            activeActivities++
        }

        fun activityStopped() {
            activeActivities = maxOf(0, activeActivities - 1)
        }
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
