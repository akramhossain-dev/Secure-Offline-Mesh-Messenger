/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.common.constants

/**
 * Application-wide string and numeric constants.
 *
 * These are pure compile-time values not derived from BuildConfig.
 * Grouped by functional area for discoverability.
 *
 * Phase A1: Foundation constants only.
 * Phase A2+: Add feature-specific constants in dedicated objects.
 */
object AppConstants {

    // ─────────────────────────────────────────────────────────────────────────
    // Application Identity
    // ─────────────────────────────────────────────────────────────────────────

    const val APP_PACKAGE = "com.mesh.emergency"
    const val APP_DISPLAY_NAME = "OfflineMesh"

    // ─────────────────────────────────────────────────────────────────────────
    // SharedPreferences / DataStore Keys
    // ─────────────────────────────────────────────────────────────────────────

    /** DataStore preferences file name. */
    const val PREFS_NAME = "mesh_preferences"

    /** Key for persisted theme mode preference. */
    const val PREF_KEY_THEME_MODE = "theme_mode"

    /** Key for persisted language preference. */
    const val PREF_KEY_LANGUAGE = "app_language"

    /** Key for dynamic color preference. */
    const val PREF_KEY_DYNAMIC_COLOR = "dynamic_color_enabled"

    // ─────────────────────────────────────────────────────────────────────────
    // BLE UUIDs (Phase A2+)
    // These match the ESP32 GATT server specification documented in
    // docs/communication/bluetooth-transport.md
    // ─────────────────────────────────────────────────────────────────────────

    const val BLE_MESH_SERVICE_UUID         = "0000182000001000800000805f9b34fb"
    const val BLE_TX_CHARACTERISTIC_UUID    = "00002a6e00001000800000805f9b34fb"
    const val BLE_RX_CHARACTERISTIC_UUID    = "00002a6f00001000800000805f9b34fb"
    const val BLE_STATUS_CHARACTERISTIC_UUID = "00002a7000001000800000805f9b34fb"

    /** ESP32 advertisement service name prefix for discovery filtering. */
    const val BLE_DEVICE_NAME_PREFIX = "MeshNode"

    // ─────────────────────────────────────────────────────────────────────────
    // LoRa Configuration (Phase A2+)
    // ─────────────────────────────────────────────────────────────────────────

    const val LORA_FREQUENCY_MHZ    = 433.0
    const val LORA_BANDWIDTH_KHZ    = 125.0
    const val LORA_SPREADING_FACTOR = 9
    const val LORA_CODING_RATE      = 5
    const val LORA_TX_POWER_DBM     = 17

    // ─────────────────────────────────────────────────────────────────────────
    // Message Constraints (Phase A2+)
    // ─────────────────────────────────────────────────────────────────────────

    /** Maximum plain-text message length in characters. */
    const val MAX_MESSAGE_LENGTH = 500

    /** Maximum voice note duration in seconds. */
    const val MAX_VOICE_DURATION_SEC = 60

    /** Node ID length (UUID without dashes). */
    const val NODE_ID_LENGTH = 16

    // ─────────────────────────────────────────────────────────────────────────
    // UI Constants
    // ─────────────────────────────────────────────────────────────────────────

    /** Default animation duration in milliseconds. */
    const val ANIMATION_DURATION_MS = 300L

    /** Debounce duration for user input in milliseconds. */
    const val INPUT_DEBOUNCE_MS = 300L

    // ─────────────────────────────────────────────────────────────────────────
    // Notification Channel IDs (Phase A2+)
    // ─────────────────────────────────────────────────────────────────────────

    const val NOTIFICATION_CHANNEL_MESSAGES  = "channel_messages"
    const val NOTIFICATION_CHANNEL_EMERGENCY = "channel_emergency"
    const val NOTIFICATION_CHANNEL_BLE       = "channel_ble_service"
}
