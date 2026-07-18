/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.common.config

import com.mesh.emergency.BuildConfig

/**
 * Feature flags for the Offline Emergency Mesh Communication System.
 *
 * All flags default to `false` in Phase A1 — no features are active yet.
 * Flags are injected via Gradle buildConfigField, making them compile-time
 * constants that R8/ProGuard can fully eliminate in release builds.
 *
 * Activation timeline:
 * - Phase A2: FEATURE_BLE, FEATURE_ENCRYPTION
 * - Phase A3: FEATURE_LORA, FEATURE_STORE_FORWARD
 * - Phase A4: FEATURE_MAPS, FEATURE_VOICE, FEATURE_QR_PAIRING
 * - Phase A5: FEATURE_EMERGENCY, FEATURE_NETWORK_DASHBOARD
 */
object FeatureFlags {

    // ─────────────────────────────────────────────────────────────────────────
    // Communication Features
    // ─────────────────────────────────────────────────────────────────────────

    /** Bluetooth BLE transport. Requires BLUETOOTH_SCAN/CONNECT permissions. */
    val FEATURE_BLE: Boolean = BuildConfig.FEATURE_BLE

    /** LoRa transport via ESP32 BLE bridge. Requires FEATURE_BLE. */
    val FEATURE_LORA: Boolean = BuildConfig.FEATURE_LORA

    /** Store & Forward message queuing. Requires FEATURE_BLE. */
    val FEATURE_STORE_FORWARD: Boolean = BuildConfig.FEATURE_BLE

    // ─────────────────────────────────────────────────────────────────────────
    // Security Features
    // ─────────────────────────────────────────────────────────────────────────

    /** AES-256-GCM end-to-end encryption. Requires Android Keystore. */
    val FEATURE_ENCRYPTION: Boolean = BuildConfig.FEATURE_ENCRYPTION

    // ─────────────────────────────────────────────────────────────────────────
    // UI / UX Features
    // ─────────────────────────────────────────────────────────────────────────

    /** Offline maps via OsmDroid. */
    val FEATURE_MAPS: Boolean = BuildConfig.FEATURE_MAPS

    /** Voice message recording and playback. */
    val FEATURE_VOICE: Boolean = BuildConfig.FEATURE_BLE

    /** QR code contact pairing. */
    val FEATURE_QR_PAIRING: Boolean = BuildConfig.FEATURE_BLE

    // ─────────────────────────────────────────────────────────────────────────
    // Emergency Features
    // ─────────────────────────────────────────────────────────────────────────

    /** Emergency SOS broadcast. */
    val FEATURE_EMERGENCY: Boolean = BuildConfig.FEATURE_BLE

    /** Location sharing. */
    val FEATURE_LOCATION: Boolean = BuildConfig.FEATURE_BLE

    // ─────────────────────────────────────────────────────────────────────────
    // Developer / Debug Features
    // ─────────────────────────────────────────────────────────────────────────

    /** Network dashboard showing mesh topology. */
    val FEATURE_NETWORK_DASHBOARD: Boolean = BuildConfig.FEATURE_BLE

    /** Power telemetry via INA219 sensor. */
    val FEATURE_POWER_TELEMETRY: Boolean = false

    /** Show developer options menu. */
    val FEATURE_DEV_OPTIONS: Boolean = BuildConfig.DEBUG
}
