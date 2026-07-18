/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.hardware.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager as AndroidBluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.mesh.emergency.core.hardware.bluetooth.BluetoothManager
import com.mesh.emergency.core.hardware.bluetooth.BluetoothState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [BluetoothManager] wrapping the Android System Bluetooth adapter.
 * Uses a broadcast receiver to listen for Bluetooth toggles on the device.
 */
@Singleton
class BluetoothManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : BluetoothManager {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? AndroidBluetoothManager
    private val adapter = bluetoothManager?.adapter

    private val _state = MutableStateFlow(getInitialState())
    override val state: StateFlow<BluetoothState> = _state.asStateFlow()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val adapterState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                _state.value = when (adapterState) {
                    BluetoothAdapter.STATE_ON  -> BluetoothState.ENABLED
                    BluetoothAdapter.STATE_OFF -> BluetoothState.DISABLED
                    else                       -> _state.value
                }
            }
        }
    }

    init {
        try {
            context.registerReceiver(receiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
        } catch (e: Exception) {
            // Registering receiver fails silently in mock environments
        }
    }

    override fun isBluetoothAvailable(): Boolean = adapter != null

    override fun isBluetoothEnabled(): Boolean = adapter?.isEnabled == true

    private fun getInitialState(): BluetoothState {
        val activeAdapter = adapter ?: return BluetoothState.NOT_SUPPORTED
        return if (activeAdapter.isEnabled) BluetoothState.ENABLED else BluetoothState.DISABLED
    }
}
