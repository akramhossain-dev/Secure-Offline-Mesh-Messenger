/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.hardware

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

// ─────────────────────────────────────────────────────────────────────────────
// A30.4 — BLE Device Model
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Represents a discovered BLE peripheral advertising the Mesh Service UUID.
 * Decoupled from [android.bluetooth.BluetoothDevice] to remain testable.
 */
data class BleDevice(
    val id: String,                         // MAC address (anonymised in logs)
    val name: String,
    val macAddress: String,
    val rssi: Int,                          // dBm
    val connectionState: BleConnectionState,
    val lastSeenMs: Long = System.currentTimeMillis(),
    val isCompatible: Boolean = true,       // true = advertises Mesh Service UUID
    val services: List<String> = emptyList() // discovered GATT service UUIDs
)

/**
 * BLE peripheral lifecycle states.
 */
enum class BleConnectionState {
    DISCOVERED,   // found in scan results, not yet connected
    CONNECTING,
    CONNECTED,
    DISCONNECTING,
    DISCONNECTED,
    FAILED        // connection attempt rejected / timeout
}

// ─────────────────────────────────────────────────────────────────────────────
// A31.1 — Hardware Device Profile
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Represents the physical hardware node (ESP32 + LoRa module) profile
 * after successful BLE service discovery.
 */
data class HardwareDeviceProfile(
    val hardwareId: String,
    val deviceType: HardwareDeviceType,
    val firmwareVersion: String,
    val batteryPercent: Int,                // 0–100
    val capabilities: Set<HardwareCapability>,
    val signalRssi: Int,
    val isConnected: Boolean,
    val lastPingMs: Long = System.currentTimeMillis()
)

enum class HardwareDeviceType {
    ESP32_LORA,       // Primary target: ESP32 + SX1278
    ESP32_ONLY,       // ESP32 without LoRa module
    GENERIC_BLE,      // Unidentified compatible BLE peripheral
    UNKNOWN
}

enum class HardwareCapability {
    LORA_TX,          // Can transmit LoRa packets
    LORA_RX,          // Can receive LoRa packets
    GPS,              // Has GPS module
    BATTERY_MONITOR,  // Has INA219 or equivalent
    MESH_RELAY        // Can forward mesh packets
}

// ─────────────────────────────────────────────────────────────────────────────
// A30.1 / A30.5 — Discovery States
// ─────────────────────────────────────────────────────────────────────────────

enum class BleDiscoveryState {
    IDLE,
    SCANNING,
    STOPPED,
    PERMISSION_DENIED,
    BLE_UNAVAILABLE
}

// ─────────────────────────────────────────────────────────────────────────────
// Repository Interface (A30.2 Transport abstraction at device level)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Contract for BLE device scanning and connection management.
 * Implementations may use real [android.bluetooth.le.BluetoothLeScanner]
 * or a simulation for testing.
 */
interface BleDeviceRepository {
    /** Stream of currently discovered/connected BLE devices. */
    val discoveredDevices: StateFlow<List<BleDevice>>

    /** Current scanning state. */
    val discoveryState: StateFlow<BleDiscoveryState>

    /** Start scanning for compatible mesh BLE peripherals. */
    suspend fun startDiscovery()

    /** Stop an active scan. */
    suspend fun stopDiscovery()

    /** Request connection to a discovered device by its MAC address. */
    suspend fun connectDevice(macAddress: String): BleConnectionResult

    /** Disconnect from a connected device. */
    suspend fun disconnectDevice(macAddress: String)

    /** Stream of raw data received from connected devices. */
    fun dataStream(): Flow<BleDataPacket>
}

/**
 * Result of a BLE connection attempt.
 */
sealed interface BleConnectionResult {
    data object Success              : BleConnectionResult
    data class Failure(val reason: BleFailureReason) : BleConnectionResult
}

enum class BleFailureReason {
    PERMISSION_DENIED,
    ADAPTER_DISABLED,
    DEVICE_NOT_FOUND,
    GATT_SERVICE_MISSING,
    TIMEOUT,
    ALREADY_CONNECTED,
    UNKNOWN_ERROR
}

/**
 * A raw data packet received from a BLE peripheral.
 */
data class BleDataPacket(
    val sourceDeviceId: String,
    val payload: ByteArray,
    val receivedAtMs: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?) = other is BleDataPacket &&
        sourceDeviceId == other.sourceDeviceId && payload.contentEquals(other.payload)
    override fun hashCode() = 31 * sourceDeviceId.hashCode() + payload.contentHashCode()
}
