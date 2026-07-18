/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.communication.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import com.mesh.emergency.core.common.result.Result
import com.mesh.emergency.core.communication.Transport
import com.mesh.emergency.core.communication.TransportStatus
import com.mesh.emergency.core.communication.TransportType
import com.mesh.emergency.core.utils.PermissionManager
import com.mesh.emergency.core.utils.permission.PermissionType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reusable BLE Transport Layer bridging communications over Android Bluetooth GATT APIs.
 * Automatically scans for ESP32/LoRa peripherals publishing the core Service UUID.
 */
@Singleton
class BluetoothTransportImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val permissionManager: PermissionManager
) : Transport {

    override val type: TransportType = TransportType.BLUETOOTH

    private val _status = MutableStateFlow(TransportStatus.DISCONNECTED)
    override val status: StateFlow<TransportStatus> = _status.asStateFlow()

    private val _receiveFlow = MutableSharedFlow<ByteArray>(extraBufferCapacity = 32)

    private val adapter: BluetoothAdapter? by lazy {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
        manager?.adapter
    }

    private var gatt: BluetoothGatt? = null
    private var rxCharacteristic: BluetoothGattCharacteristic? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    // GATT Service and Characteristic UUIDs for Emergency Mesh
    private val SERVICE_UUID = UUID.fromString("0000FF10-0000-1000-8000-00805F9B34FB")
    private val RX_CHAR_UUID = UUID.fromString("0000FF11-0000-1000-8000-00805F9B34FB") // Write
    private val TX_CHAR_UUID = UUID.fromString("0000FF12-0000-1000-8000-00805F9B34FB") // Notify/Read
    private val CLIENT_CONFIG_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")

    @SuppressLint("MissingPermission")
    override suspend fun connect(): Result<Unit> {
        if (!permissionManager.hasPermission(context, PermissionType.BLUETOOTH)) {
            return Result.Error(Exception("Bluetooth permissions denied"))
        }

        val activeAdapter = adapter ?: return Result.Error(Exception("Bluetooth not supported"))
        if (!activeAdapter.isEnabled) {
            return Result.Error(Exception("Bluetooth is disabled"))
        }

        if (_status.value == TransportStatus.CONNECTED || _status.value == TransportStatus.CONNECTING) {
            return Result.Success(Unit)
        }

        _status.value = TransportStatus.CONNECTING

        // Start scanning for nearby ESP32 targets
        val targetDevice = withTimeoutOrNull(10000L) {
            var foundDevice: android.bluetooth.BluetoothDevice? = null
            val scanner = activeAdapter.bluetoothLeScanner
            val scanCallback = object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult?) {
                    result?.device?.let { device ->
                        foundDevice = device
                    }
                }
            }

            val filter = ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(SERVICE_UUID))
                .build()

            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()

            scanner?.startScan(listOf(filter), settings, scanCallback)

            while (foundDevice == null) {
                delay(100L)
            }

            scanner?.stopScan(scanCallback)
            foundDevice
        }

        if (targetDevice == null) {
            _status.value = TransportStatus.DISCONNECTED
            return Result.Error(Exception("No mesh peripherals detected in range"))
        }

        // Establish GATT Connection
        gatt = targetDevice.connectGatt(context, false, gattCallback)
        return Result.Success(Unit)
    }

    @SuppressLint("MissingPermission")
    override suspend fun disconnect(): Result<Unit> {
        gatt?.disconnect()
        gatt?.close()
        gatt = null
        rxCharacteristic = null
        _status.value = TransportStatus.DISCONNECTED
        return Result.Success(Unit)
    }

    @SuppressLint("MissingPermission")
    override suspend fun send(data: ByteArray): Result<Unit> {
        val activeGatt = gatt ?: return Result.Error(Exception("GATT not connected"))
        val char = rxCharacteristic ?: return Result.Error(Exception("Rx characteristic unavailable"))

        char.value = data
        char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        val success = activeGatt.writeCharacteristic(char)

        return if (success) Result.Success(Unit) else Result.Error(Exception("GATT write failed"))
    }

    override fun receive(): Flow<ByteArray> = _receiveFlow.asSharedFlow()

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    gatt?.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    _status.value = TransportStatus.DISCONNECTED
                    this@BluetoothTransportImpl.gatt = null
                    rxCharacteristic = null
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt?.getService(SERVICE_UUID)
                rxCharacteristic = service?.getCharacteristic(RX_CHAR_UUID)
                val txChar = service?.getCharacteristic(TX_CHAR_UUID)

                // Enable GATT Notifications on the Tx Characteristic
                if (txChar != null) {
                    gatt.setCharacteristicNotification(txChar, true)
                    val descriptor = txChar.getDescriptor(CLIENT_CONFIG_DESCRIPTOR_UUID)
                    descriptor?.let {
                        it.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        gatt.writeDescriptor(it)
                    }
                    _status.value = TransportStatus.CONNECTED
                } else {
                    _status.value = TransportStatus.DISCONNECTED
                }
            } else {
                _status.value = TransportStatus.DISCONNECTED
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            if (characteristic?.uuid == TX_CHAR_UUID) {
                val data = characteristic.value
                if (data != null) {
                    _receiveFlow.tryEmit(data)
                }
            }
        }
    }
}
