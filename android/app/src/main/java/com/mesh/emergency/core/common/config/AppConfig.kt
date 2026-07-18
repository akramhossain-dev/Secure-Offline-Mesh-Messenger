/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.common.config

import com.mesh.emergency.BuildConfig

/**
 * Application-wide configuration constants derived from [BuildConfig].
 *
 * All values are compile-time constants injected by the Gradle build system.
 * No business logic lives here — this is a pure configuration object.
 *
 * Phase A1: Foundation constants only.
 * Phase A2+: Will add feature-specific configuration.
 */
object AppConfig {

    // ─────────────────────────────────────────────────────────────────────────
    // App Identity
    // ─────────────────────────────────────────────────────────────────────────

    /** Human-readable application name. */
    const val APP_NAME: String = "Offline Emergency Mesh"

    /** Application package identifier. */
    const val PACKAGE_NAME: String = "com.mesh.emergency"

    /** Current environment label (injected from buildConfigField). */
    val ENVIRONMENT: String = BuildConfig.ENVIRONMENT

    // ─────────────────────────────────────────────────────────────────────────
    // Communication Timeouts (milliseconds)
    // These will be used by the Communication Manager in Phase A2.
    // ─────────────────────────────────────────────────────────────────────────

    /** BLE connection and operation timeout in milliseconds. */
    val BLE_TIMEOUT_MS: Long = BuildConfig.BLE_TIMEOUT_MS

    /** LoRa message delivery timeout in milliseconds. */
    val LORA_TIMEOUT_MS: Long = BuildConfig.LORA_TIMEOUT_MS

    /** Maximum number of mesh hops allowed for a message. */
    val MAX_HOPS: Int = BuildConfig.MAX_HOPS

    // ─────────────────────────────────────────────────────────────────────────
    // Store & Forward Configuration (Phase A2+)
    // ─────────────────────────────────────────────────────────────────────────

    /** Maximum number of pending messages in the Store & Forward queue. */
    const val MAX_QUEUE_SIZE: Int = 500

    /** Message retry interval in milliseconds. */
    const val RETRY_INTERVAL_MS: Long = 15_000L

    /** Maximum message age in milliseconds before expiry (72 hours). */
    const val MESSAGE_TTL_MS: Long = 72L * 60L * 60L * 1_000L

    // ─────────────────────────────────────────────────────────────────────────
    // BLE GATT Configuration (Phase A2+)
    // ─────────────────────────────────────────────────────────────────────────

    /** BLE MTU size for GATT characteristic writes. */
    const val BLE_MTU_SIZE: Int = 512

    /** Packet fragment size for large payloads. */
    const val PACKET_FRAGMENT_SIZE: Int = 512

    // ─────────────────────────────────────────────────────────────────────────
    // Encryption Configuration (Phase A2+)
    // ─────────────────────────────────────────────────────────────────────────

    /** AES-GCM key size in bits. */
    const val AES_KEY_SIZE_BITS: Int = 256

    /** AES-GCM nonce size in bytes. */
    const val AES_NONCE_SIZE_BYTES: Int = 12

    /** AES-GCM authentication tag size in bytes. */
    const val AES_TAG_SIZE_BYTES: Int = 16
}
