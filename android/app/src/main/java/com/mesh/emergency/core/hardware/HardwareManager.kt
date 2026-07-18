/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.hardware

import kotlinx.coroutines.flow.StateFlow

// ─────────────────────────────────────────────────────────────────────────────
// A30.8 — HardwareManager interface
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Top-level hardware coordination layer.
 *
 * Sits above [BleDeviceRepository] and [Esp32CommandProtocol] to provide
 * a unified API to the rest of the application. The UI and feature modules
 * talk to HardwareManager, never directly to BLE primitives.
 *
 * Architecture:
 * ```
 *   ViewModel
 *      │
 *   HardwareManager
 *      ├── BleDeviceRepository  (scan / connect / data stream)
 *      └── Esp32CommandProtocol (encode / decode commands)
 * ```
 */
interface HardwareManager {

    /** Live list of all currently known BLE nodes (discovered + connected). */
    val knownDevices: StateFlow<List<BleDevice>>

    /** Profile of the currently connected primary hardware node, or null. */
    val connectedProfile: StateFlow<HardwareDeviceProfile?>

    /** Current BLE discovery/scanning state. */
    val discoveryState: StateFlow<BleDiscoveryState>

    /**
     * Start scanning for compatible mesh peripherals.
     * Automatically stops after [timeoutMs] if no device is found.
     */
    suspend fun startDiscovery(timeoutMs: Long = 15_000L)

    /** Stop an ongoing BLE scan. */
    suspend fun stopDiscovery()

    /**
     * Connect to a discovered peripheral by its MAC address.
     * On success, queries the hardware profile via [Esp32CommandProtocol].
     */
    suspend fun connectToDevice(macAddress: String): BleConnectionResult

    /** Gracefully disconnect from the currently connected peripheral. */
    suspend fun disconnectCurrentDevice()

    /**
     * Send a raw command packet to the connected ESP32 peripheral.
     * Returns an error result if no device is connected.
     */
    suspend fun sendCommand(command: Esp32Command): HardwareCommandResult

    /**
     * Request a fresh hardware status update from the connected peripheral.
     * Emits to [connectedProfile].
     */
    suspend fun refreshHardwareStatus()
}

/**
 * Result of a command sent to the hardware peripheral.
 */
sealed interface HardwareCommandResult {
    data class Success(val response: ByteArray?)   : HardwareCommandResult
    data class Failure(val reason: String)         : HardwareCommandResult
    data object NoDeviceConnected                   : HardwareCommandResult
}
