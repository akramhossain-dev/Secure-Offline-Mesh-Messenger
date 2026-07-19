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
import com.mesh.emergency.data.communication.bluetooth.BluetoothTransportImpl
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
     * kicking off the persistent BLE connect() retry loop on every phone.
     */
    @Inject
    lateinit var communicationManager: CommunicationManagerImpl

    /**
     * Eagerly injected to start GATT server + advertising immediately at launch,
     * independently of [communicationManager]. The advertising is critical for
     * being discoverable as the QR-shower (Phone B).
     */
    @Inject
    lateinit var bluetoothTransport: BluetoothTransportImpl

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    private val appScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        initializeLogging()
        // Initialize global CrashHandler with application context for local crash log persistence
        com.mesh.emergency.core.error.CrashHandler.install(this)

        // Schedule all periodic background sync, routing, and cleanup workers
        meshWorkManager.scheduleAllWorkers()

        // Eagerly start BLE: both phones must advertise from the very first second.
        // Without this, Phone B (QR shower / Profile screen) is never advertising,
        // so Phone A's 15s reconnect scan after QR pairing finds nothing.
        appScope.launch {
            try {
                Timber.d("BLE_FLOW: MeshApplication eagerly starting BLE transport")
                bluetoothTransport.connect()
            } catch (e: Exception) {
                Timber.e(e, "BLE_FLOW: Eager BLE connect() failed in Application.onCreate()")
            }
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
