/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.system

import android.bluetooth.BluetoothManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [BluetoothServiceWrapper] query-mapping states to [BluetoothManager].
 */
@Singleton
class BluetoothServiceWrapperImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : BluetoothServiceWrapper {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager

    override fun isBluetoothEnabled(): Boolean {
        return bluetoothManager?.adapter?.isEnabled == true
    }

    override fun enableBluetooth() {
        // Platform level bluetooth enabling requires user intents on modern APIs.
        // This is a stub placeholder mapping intent updates in later phases.
    }

    override fun disableBluetooth() {
        // Platform level bluetooth disabling wrapper placeholder.
    }
}
