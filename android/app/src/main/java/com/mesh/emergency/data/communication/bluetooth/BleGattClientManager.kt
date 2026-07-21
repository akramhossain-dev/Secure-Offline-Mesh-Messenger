/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.communication.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Encapsulates BLE peripheral scanning, GATT Client connections, characteristic discovery,
 * and client characteristic writes for Bluetooth transport.
 */
@Singleton
class BleGattClientManager @Inject constructor() {

    var gatt: BluetoothGatt? = null
        private set

    var rxCharacteristic: BluetoothGattCharacteristic? = null
        private set

    private var scanner: BluetoothLeScanner? = null
    private var isScanning = false

    @SuppressLint("MissingPermission")
    fun startScanning(
        adapter: BluetoothAdapter?,
        onDeviceDiscovered: (BluetoothDevice, Int) -> Unit
    ) {
        if (adapter == null || !adapter.isEnabled) {
            Timber.w("BleGattClientManager: Bluetooth disabled, scanning aborted")
            return
        }

        scanner = adapter.bluetoothLeScanner
        if (scanner == null) {
            Timber.w("BleGattClientManager: BluetoothLeScanner unavailable")
            return
        }

        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(BleGattServerManager.MESH_SERVICE_UUID))
            .build()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        val scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                val device = result?.device ?: return
                onDeviceDiscovered(device, result.rssi)
            }

            override fun onScanFailed(errorCode: Int) {
                Timber.e("BleGattClientManager: Scan failed with error code: $errorCode")
            }
        }

        try {
            isScanning = true
            scanner?.startScan(listOf(filter), settings, scanCallback)
            Timber.d("BleGattClientManager: BLE Scanning started successfully.")
        } catch (e: Exception) {
            Timber.e(e, "BleGattClientManager: Error starting scan")
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScanning(scanCallback: ScanCallback) {
        if (isScanning) {
            try {
                scanner?.stopScan(scanCallback)
                isScanning = false
                Timber.d("BleGattClientManager: BLE Scanning stopped.")
            } catch (e: Exception) {
                Timber.e(e, "BleGattClientManager: Error stopping scan")
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(
        context: Context,
        device: BluetoothDevice,
        gattCallback: BluetoothGattCallback
    ): Boolean {
        try {
            disconnect()
            gatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
            Timber.d("BleGattClientManager: Connecting to device ${device.address}...")
            return true
        } catch (e: Exception) {
            Timber.e(e, "BleGattClientManager: Failed to connect to device ${device.address}")
            return false
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        try {
            gatt?.disconnect()
            gatt?.close()
            gatt = null
            rxCharacteristic = null
            Timber.d("BleGattClientManager: GATT Client disconnected.")
        } catch (e: Exception) {
            Timber.e(e, "BleGattClientManager: Error disconnecting GATT client")
        }
    }

    @SuppressLint("MissingPermission")
    fun onServicesDiscovered(discoveredGatt: BluetoothGatt): Boolean {
        val service = discoveredGatt.getService(BleGattServerManager.MESH_SERVICE_UUID)
        if (service == null) {
            Timber.w("BleGattClientManager: Mesh service not found on remote GATT server")
            return false
        }

        rxCharacteristic = service.getCharacteristic(BleGattServerManager.RX_CHAR_UUID)
        val txChar = service.getCharacteristic(BleGattServerManager.TX_CHAR_UUID)

        if (txChar != null) {
            discoveredGatt.setCharacteristicNotification(txChar, true)
            val descriptor = txChar.getDescriptor(BleGattServerManager.CCCD_UUID)
            if (descriptor != null) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    discoveredGatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                } else {
                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    discoveredGatt.writeDescriptor(descriptor)
                }
                Timber.d("BleGattClientManager: Enabled TX notifications on remote GATT server")
            }
        }
        return true
    }

    @SuppressLint("MissingPermission")
    fun sendViaGattClient(payload: ByteArray): Boolean {
        val activeGatt = gatt ?: return false
        val rxChar = rxCharacteristic ?: return false

        return try {
            val success = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                val status = activeGatt.writeCharacteristic(rxChar, payload, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                status == android.bluetooth.BluetoothStatusCodes.SUCCESS
            } else {
                rxChar.value = payload
                rxChar.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                activeGatt.writeCharacteristic(rxChar)
            }
            Timber.d("BleGattClientManager: Write characteristic to server success=$success")
            success
        } catch (e: Exception) {
            Timber.e(e, "BleGattClientManager: Failed write to remote server")
            false
        }
    }
}
