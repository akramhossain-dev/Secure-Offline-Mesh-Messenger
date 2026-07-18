/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.hardware.bluetooth

/**
 * Discovered BLE node info parameters.
 */
data class BluetoothDeviceInfo(
    val deviceId: String,
    val name: String,
    val rssi: Int,
    val isConnected: Boolean
)
