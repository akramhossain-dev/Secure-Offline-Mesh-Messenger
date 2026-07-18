/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.common.config

import com.mesh.emergency.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Decoupled implementation querying app attributes directly from [BuildConfig] and static helpers.
 */
@Singleton
class AppConfigurationImpl @Inject constructor() : AppConfiguration {
    override val isDebug: Boolean = BuildConfig.DEBUG
    override val isRelease: Boolean = !BuildConfig.DEBUG
    override val versionName: String = BuildConfig.VERSION_NAME
    override val versionCode: Int = BuildConfig.VERSION_CODE
    override val applicationId: String = BuildConfig.APPLICATION_ID
    override val environment: String = BuildConfig.ENVIRONMENT
    override val enableLogging: Boolean = BuildConfig.ENABLE_LOGGING
    override val bleTimeoutMs: Long = BuildConfig.BLE_TIMEOUT_MS
    override val loraTimeoutMs: Long = BuildConfig.LORA_TIMEOUT_MS
    override val maxHops: Int = BuildConfig.MAX_HOPS

    override fun isFeatureEnabled(feature: Feature): Boolean {
        return when (feature) {
            Feature.BLE -> BuildConfig.FEATURE_BLE
            Feature.LORA -> BuildConfig.FEATURE_LORA
            Feature.STORE_FORWARD -> false
            Feature.ENCRYPTION -> BuildConfig.FEATURE_ENCRYPTION
            Feature.MAPS -> BuildConfig.FEATURE_MAPS
            Feature.VOICE -> false
            Feature.QR_PAIRING -> false
            Feature.EMERGENCY -> false
            Feature.LOCATION -> false
            Feature.NETWORK_DASHBOARD -> false
            Feature.POWER_TELEMETRY -> false
            Feature.DEV_OPTIONS -> BuildConfig.DEBUG
        }
    }
}
