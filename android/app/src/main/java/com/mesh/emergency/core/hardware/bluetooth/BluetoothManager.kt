/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.hardware.bluetooth

import kotlinx.coroutines.flow.StateFlow

/**
 * Abstraction wrapping android Bluetooth adapter state query operations.
 */
interface BluetoothManager {
    /** Exposes active state of Bluetooth transceiver. */
    val state: StateFlow<BluetoothState>

    /** Returns true if Bluetooth hardware exists on the device. */
    fun isBluetoothAvailable(): Boolean

    /** Returns true if Bluetooth is currently toggled on. */
    fun isBluetoothEnabled(): Boolean
}
