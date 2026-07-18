/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.system

/**
 * Interface contract wrapping Android BluetoothManager calls.
 */
interface BluetoothServiceWrapper {
    /** Returns true if Bluetooth is powered on. */
    fun isBluetoothEnabled(): Boolean

    /** Requests system to turn on Bluetooth. */
    fun enableBluetooth()

    /** Requests system to turn off Bluetooth. */
    fun disableBluetooth()
}
