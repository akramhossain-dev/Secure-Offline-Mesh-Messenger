/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.common.config

/**
 * Interface contract for build-time and feature configuration parameters.
 */
interface AppConfiguration {
    /** True if run in debug. */
    val isDebug: Boolean

    /** True if run in release. */
    val isRelease: Boolean

    /** Build version string. */
    val versionName: String

    /** Build version code. */
    val versionCode: Int

    /** Package namespace identifier. */
    val applicationId: String

    /** Active environment label. */
    val environment: String

    /** Toggle logs printing. */
    val enableLogging: Boolean

    /** Connect timeout boundary for BLE. */
    val bleTimeoutMs: Long

    /** Connect timeout boundary for LoRa. */
    val loraTimeoutMs: Long

    /** Max hops limit. */
    val maxHops: Int

    /** Check if a feature is enabled at runtime. */
    fun isFeatureEnabled(feature: Feature): Boolean
}

/**
 * Application feature toggles list.
 */
enum class Feature {
    BLE,
    LORA,
    STORE_FORWARD,
    ENCRYPTION,
    MAPS,
    VOICE,
    QR_PAIRING,
    EMERGENCY,
    LOCATION,
    NETWORK_DASHBOARD,
    POWER_TELEMETRY,
    DEV_OPTIONS
}
