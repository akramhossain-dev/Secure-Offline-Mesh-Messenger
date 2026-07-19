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
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
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
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reusable BLE Transport Layer bridging communications over Android Bluetooth GATT APIs.
 * Automatically advertises emergency services and opens a GATT server for peer-to-peer phone connection,
 * while simultaneously scanning for nearby peripherals publishing the core Service UUID.
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

    private val bluetoothManager: BluetoothManager? by lazy {
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    }

    private val adapter: BluetoothAdapter? by lazy {
        bluetoothManager?.adapter
    }

    // GATT Client fields
    private var gatt: BluetoothGatt? = null
    private var rxCharacteristic: BluetoothGattCharacteristic? = null

    // GATT Server & Advertising fields
    private var gattServer: BluetoothGattServer? = null
    private var txServerCharacteristic: BluetoothGattCharacteristic? = null
    private val connectedDevices = mutableSetOf<BluetoothDevice>()

    private val scope = CoroutineScope(Dispatchers.IO)

    // GATT Service and Characteristic UUIDs for Emergency Mesh
    private val SERVICE_UUID = UUID.fromString("0000FF10-0000-1000-8000-00805F9B34FB")
    private val RX_CHAR_UUID = UUID.fromString("0000FF11-0000-1000-8000-00805F9B34FB") // Client writes here
    private val TX_CHAR_UUID = UUID.fromString("0000FF12-0000-1000-8000-00805F9B34FB") // Server notifies client here
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

        // 1. Initialize GATT Server and start BLE Advertising
        startGattServer()
        startAdvertising()

        // 2. Start scanning for other phones acting as servers
        val targetDevice = withTimeoutOrNull(10000L) {
            var foundDevice: BluetoothDevice? = null
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

        if (targetDevice != null) {
            Timber.d("BluetoothTransport: Found mesh partner device ${targetDevice.address}. Establishing client connection...")
            gatt = targetDevice.connectGatt(context, false, gattCallback)
        } else {
            Timber.d("BluetoothTransport: Scanning timed out. Operating in GATT Server mode awaiting connections.")
            // Remain in connecting/waiting status since we have the server and advertisement active
            if (connectedDevices.isNotEmpty()) {
                _status.value = TransportStatus.CONNECTED
            }
        }

        return Result.Success(Unit)
    }

    @SuppressLint("MissingPermission")
    override suspend fun disconnect(): Result<Unit> {
        // Stop Advertising
        stopAdvertising()

        // Close GATT Server
        gattServer?.let {
            it.clearServices()
            it.close()
        }
        gattServer = null
        connectedDevices.clear()
        txServerCharacteristic = null

        // Disconnect Client
        gatt?.disconnect()
        gatt?.close()
        gatt = null
        rxCharacteristic = null

        _status.value = TransportStatus.DISCONNECTED
        return Result.Success(Unit)
    }

    @SuppressLint("MissingPermission")
    override suspend fun send(data: ByteArray): Result<Unit> {
        // Scenario A: Write to the target device's GATT server if we are connected as a client
        val activeGatt = gatt
        val clientChar = rxCharacteristic
        if (activeGatt != null && clientChar != null) {
            clientChar.value = data
            clientChar.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            val success = activeGatt.writeCharacteristic(clientChar)
            if (success) {
                Timber.d("BluetoothTransport: Message sent successfully as client.")
                return Result.Success(Unit)
            }
        }

        // Scenario B: Notify all connected clients if we are acting as a GATT server
        val activeServer = gattServer
        val serverChar = txServerCharacteristic
        if (activeServer != null && serverChar != null && connectedDevices.isNotEmpty()) {
            serverChar.value = data
            var successCount = 0
            for (device in connectedDevices) {
                val success = activeServer.notifyCharacteristicChanged(device, serverChar, false)
                if (success) successCount++
            }
            if (successCount > 0) {
                Timber.d("BluetoothTransport: Message notified to $successCount server clients.")
                return Result.Success(Unit)
            }
        }

        return Result.Error(Exception("GATT channel unavailable (no active client or server connections)"))
    }

    override fun receive(): Flow<ByteArray> = _receiveFlow.asSharedFlow()

    // ── GATT Server & Advertising Methods ───────────────────────────────────────────

    @SuppressLint("MissingPermission")
    private fun startAdvertising() {
        val advertiser = adapter?.bluetoothLeAdvertiser ?: return
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(true)
            .build()

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .addServiceUuid(ParcelUuid(SERVICE_UUID))
            .build()

        advertiser.startAdvertising(settings, data, advertiseCallback)
        Timber.d("BluetoothTransport: BLE Advertising started.")
    }

    @SuppressLint("MissingPermission")
    private fun stopAdvertising() {
        val advertiser = adapter?.bluetoothLeAdvertiser ?: return
        advertiser.stopAdvertising(advertiseCallback)
        Timber.d("BluetoothTransport: BLE Advertising stopped.")
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            Timber.d("BluetoothTransport: Advertisement setup success.")
        }

        override fun onStartFailure(errorCode: Int) {
            Timber.e("BluetoothTransport: Advertisement setup failed. Code: $errorCode")
        }
    }

    @SuppressLint("MissingPermission")
    private fun startGattServer() {
        val bm = bluetoothManager ?: return
        gattServer = bm.openGattServer(context, gattServerCallback)

        val service = BluetoothGattService(SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)

        // Rx (Write) Characteristic
        val rxChar = BluetoothGattCharacteristic(
            RX_CHAR_UUID,
            BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )

        // Tx (Notify/Read) Characteristic
        val txChar = BluetoothGattCharacteristic(
            TX_CHAR_UUID,
            BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ
        )

        // Add descriptor to TX characteristic so clients can enable notifications
        val descriptor = BluetoothGattDescriptor(
            CLIENT_CONFIG_DESCRIPTOR_UUID,
            BluetoothGattDescriptor.PERMISSION_WRITE or BluetoothGattDescriptor.PERMISSION_READ
        )
        txChar.addDescriptor(descriptor)

        service.addCharacteristic(rxChar)
        service.addCharacteristic(txChar)

        txServerCharacteristic = txChar
        gattServer?.addService(service)
        Timber.d("BluetoothTransport: GATT Server started.")
    }

    private val gattServerCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
            if (device == null) return
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    connectedDevices.add(device)
                    _status.value = TransportStatus.CONNECTED
                    Timber.d("BluetoothTransport Server: Client device connected: ${device.address}")
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    connectedDevices.remove(device)
                    if (connectedDevices.isEmpty() && gatt == null) {
                        _status.value = TransportStatus.DISCONNECTED
                    }
                    Timber.d("BluetoothTransport Server: Client device disconnected: ${device.address}")
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice?,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic?,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            if (characteristic?.uuid == RX_CHAR_UUID && value != null) {
                Timber.d("BluetoothTransport Server: Received write request on Rx Characteristic.")
                _receiveFlow.tryEmit(value)
                if (responseNeeded) {
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
                }
            } else {
                if (responseNeeded) {
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, offset, null)
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onDescriptorWriteRequest(
            device: BluetoothDevice?,
            requestId: Int,
            descriptor: BluetoothGattDescriptor?,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            if (descriptor?.uuid == CLIENT_CONFIG_DESCRIPTOR_UUID && value != null) {
                descriptor.value = value
                if (responseNeeded) {
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
                }
                Timber.d("BluetoothTransport Server: Client configured descriptor notifications.")
            } else {
                if (responseNeeded) {
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, offset, null)
                }
            }
        }
    }

    // ── GATT Client Callbacks ───────────────────────────────────────────────────────

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    gatt?.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    _status.value = if (connectedDevices.isNotEmpty()) TransportStatus.CONNECTED else TransportStatus.DISCONNECTED
                    this@BluetoothTransportImpl.gatt = null
                    rxCharacteristic = null
                    Timber.d("BluetoothTransport Client: Disconnected from server.")
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
                    Timber.d("BluetoothTransport Client: Services discovered. Subscribed to Tx Notifications.")
                } else {
                    _status.value = if (connectedDevices.isNotEmpty()) TransportStatus.CONNECTED else TransportStatus.DISCONNECTED
                }
            } else {
                _status.value = if (connectedDevices.isNotEmpty()) TransportStatus.CONNECTED else TransportStatus.DISCONNECTED
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            if (characteristic?.uuid == TX_CHAR_UUID) {
                val data = characteristic.value
                if (data != null) {
                    Timber.d("BluetoothTransport Client: Received notification from server.")
                    _receiveFlow.tryEmit(data)
                }
            }
        }
    }
}
