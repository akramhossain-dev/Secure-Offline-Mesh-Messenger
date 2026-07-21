/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.communication.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.ParcelUuid
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Encapsulates BLE GATT Server hosting, service advertising, connected client management,
 * and server characteristic notifications for Bluetooth transport.
 */
@Singleton
class BleGattServerManager @Inject constructor() {

    companion object {
        val MESH_SERVICE_UUID: UUID = UUID.fromString("0000fe00-0000-1000-8000-00805f9b34fb")
        val RX_CHAR_UUID: UUID      = UUID.fromString("0000fe01-0000-1000-8000-00805f9b34fb")
        val TX_CHAR_UUID: UUID      = UUID.fromString("0000fe02-0000-1000-8000-00805f9b34fb")
        val CCCD_UUID: UUID         = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    private var gattServer: BluetoothGattServer? = null
    private var txServerCharacteristic: BluetoothGattCharacteristic? = null
    private var advertiser: BluetoothLeAdvertiser? = null

    val connectedClients: MutableSet<BluetoothDevice> = ConcurrentHashMap.newKeySet()

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            Timber.d("BleGattServerManager: BLE Advertising started successfully.")
        }

        override fun onStartFailure(errorCode: Int) {
            Timber.e("BleGattServerManager: BLE Advertising failed with error code: $errorCode")
        }
    }

    @SuppressLint("MissingPermission")
    fun startServer(
        context: Context,
        bluetoothManager: BluetoothManager?,
        adapter: BluetoothAdapter?,
        callback: BluetoothGattServerCallback
    ): Boolean {
        if (bluetoothManager == null || adapter == null || !adapter.isEnabled) {
            Timber.w("BleGattServerManager: Bluetooth unavailable or disabled")
            return false
        }

        try {
            gattServer = bluetoothManager.openGattServer(context, callback)
            if (gattServer == null) {
                Timber.e("BleGattServerManager: Failed to open GATT Server")
                return false
            }

            val service = BluetoothGattService(
                MESH_SERVICE_UUID,
                BluetoothGattService.SERVICE_TYPE_PRIMARY
            )

            val rxChar = BluetoothGattCharacteristic(
                RX_CHAR_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                BluetoothGattCharacteristic.PERMISSION_WRITE
            )

            val txChar = BluetoothGattCharacteristic(
                TX_CHAR_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ
            )

            val cccd = BluetoothGattDescriptor(
                CCCD_UUID,
                BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE
            )
            txChar.addDescriptor(cccd)

            service.addCharacteristic(rxChar)
            service.addCharacteristic(txChar)
            txServerCharacteristic = txChar

            gattServer?.addService(service)
            Timber.d("BleGattServerManager: GATT Server opened and service added successfully.")

            startAdvertising(adapter)
            return true
        } catch (e: Exception) {
            Timber.e(e, "BleGattServerManager: Exception starting GATT server")
            return false
        }
    }

    @SuppressLint("MissingPermission")
    fun startAdvertising(adapter: BluetoothAdapter?) {
        if (adapter == null || !adapter.isEnabled) return
        advertiser = adapter.bluetoothLeAdvertiser
        if (advertiser == null) {
            Timber.w("BleGattServerManager: BLE Advertiser unavailable")
            return
        }

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setConnectable(true)
            .setTimeout(0)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .build()

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .addServiceUuid(ParcelUuid(MESH_SERVICE_UUID))
            .build()

        try {
            advertiser?.startAdvertising(settings, data, advertiseCallback)
        } catch (e: Exception) {
            Timber.e(e, "BleGattServerManager: Error starting advertising")
        }
    }

    @SuppressLint("MissingPermission")
    fun stopAdvertising() {
        try {
            advertiser?.stopAdvertising(advertiseCallback)
        } catch (e: Exception) {
            Timber.e(e, "BleGattServerManager: Error stopping advertising")
        }
    }

    @SuppressLint("MissingPermission")
    fun stopServer() {
        stopAdvertising()
        try {
            connectedClients.clear()
            gattServer?.close()
            gattServer = null
            txServerCharacteristic = null
            Timber.d("BleGattServerManager: GATT Server stopped.")
        } catch (e: Exception) {
            Timber.e(e, "BleGattServerManager: Error closing GATT server")
        }
    }

    @SuppressLint("MissingPermission")
    fun sendViaConnectedClients(payload: ByteArray): Boolean {
        val server = gattServer ?: return false
        val txChar = txServerCharacteristic ?: return false
        if (connectedClients.isEmpty()) return false

        var anySuccess = false
        for (device in connectedClients) {
            try {
                val notifySuccess = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    val status = server.notifyCharacteristicChanged(device, txChar, false, payload)
                    status == android.bluetooth.BluetoothStatusCodes.SUCCESS
                } else {
                    txChar.value = payload
                    server.notifyCharacteristicChanged(device, txChar, false)
                }
                if (notifySuccess) {
                    anySuccess = true
                    Timber.d("BleGattServerManager: Notify success to client ${device.address}")
                }
            } catch (e: Exception) {
                Timber.e(e, "BleGattServerManager: Failed to notify client ${device.address}")
            }
        }
        return anySuccess
    }

    @SuppressLint("MissingPermission")
    fun sendResponse(device: BluetoothDevice, requestId: Int, status: Int, offset: Int, value: ByteArray?) {
        gattServer?.sendResponse(device, requestId, status, offset, value)
    }
}
