/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.hardware.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import com.mesh.emergency.core.hardware.BleConnectionResult
import com.mesh.emergency.core.hardware.BleDataPacket
import com.mesh.emergency.core.hardware.BleDevice
import com.mesh.emergency.core.hardware.BleConnectionState
import com.mesh.emergency.core.hardware.BleDeviceRepository
import com.mesh.emergency.core.hardware.BleDiscoveryState
import com.mesh.emergency.core.hardware.BleFailureReason
import com.mesh.emergency.core.communication.Transport
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A30.3 — BLE Provider implementation backed by Android BLE LE Scanner.
 *
 * Filters scan results by the Mesh GATT Service UUID so only compatible
 * ESP32/LoRa peripherals appear in the device list. The actual GATT
 * connection is delegated to the Bluetooth [Transport] implementation which
 * manages the GATT callbacks and characteristic I/O.
 */
@Singleton
class BleDeviceRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @javax.inject.Named("bluetooth") private val bluetoothTransport: Transport
) : BleDeviceRepository {

    companion object {
        /** Emergency Mesh primary GATT service UUID — must match ESP32 firmware. */
        val MESH_SERVICE_UUID: UUID = UUID.fromString("0000FF10-0000-1000-8000-00805F9B34FB")

        /** Scan auto-stop timeout (ms). Prevents battery drain. */
        private const val SCAN_TIMEOUT_MS = 15_000L
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _discoveredDevices = MutableStateFlow<List<BleDevice>>(emptyList())
    override val discoveredDevices: StateFlow<List<BleDevice>> = _discoveredDevices.asStateFlow()

    private val _discoveryState = MutableStateFlow(BleDiscoveryState.IDLE)
    override val discoveryState: StateFlow<BleDiscoveryState> = _discoveryState.asStateFlow()

    private val _dataStream = MutableSharedFlow<BleDataPacket>(extraBufferCapacity = 64)

    /** Internal mutable map so we can update individual device states atomically. */
    private val deviceMap = ConcurrentHashMap<String, BleDevice>()

    private val adapter: BluetoothAdapter? by lazy {
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
    }

    private var activeScanCallback: ScanCallback? = null

    // ── A30.5 Discovery ───────────────────────────────────────────────────────

    @SuppressLint("MissingPermission")
    override suspend fun startDiscovery() {
        val bleAdapter = adapter
        if (bleAdapter == null || !bleAdapter.isEnabled) {
            _discoveryState.value = BleDiscoveryState.BLE_UNAVAILABLE
            return
        }

        if (_discoveryState.value == BleDiscoveryState.SCANNING) return

        _discoveryState.value = BleDiscoveryState.SCANNING

        val scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                result ?: return
                val device = result.device ?: return
                val macAddress = device.address ?: return

                val bleDevice = BleDevice(
                    id              = macAddress,
                    name            = device.name ?: "Mesh-Node-${macAddress.takeLast(5)}",
                    macAddress      = macAddress,
                    rssi            = result.rssi,
                    connectionState = BleConnectionState.DISCOVERED,
                    lastSeenMs      = System.currentTimeMillis(),
                    isCompatible    = true,
                    services        = result.scanRecord?.serviceUuids?.map { it.uuid.toString() } ?: emptyList()
                )

                deviceMap[macAddress] = bleDevice
                _discoveredDevices.value = deviceMap.values.toList()
                    .sortedByDescending { it.rssi }
            }

            override fun onScanFailed(errorCode: Int) {
                _discoveryState.value = BleDiscoveryState.IDLE
            }
        }

        activeScanCallback = scanCallback

        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(MESH_SERVICE_UUID))
            .build()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(0)
            .build()

        bleAdapter.bluetoothLeScanner?.startScan(listOf(filter), settings, scanCallback)

        // Auto-stop after timeout to preserve battery
        scope.launch {
            delay(SCAN_TIMEOUT_MS)
            if (_discoveryState.value == BleDiscoveryState.SCANNING) {
                stopDiscovery()
            }
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun stopDiscovery() {
        activeScanCallback?.let { cb ->
            adapter?.bluetoothLeScanner?.stopScan(cb)
            activeScanCallback = null
        }
        _discoveryState.value = BleDiscoveryState.STOPPED
    }

    // ── A30.6 Connection Management ───────────────────────────────────────────

    override suspend fun connectDevice(macAddress: String): BleConnectionResult {
        val device = deviceMap[macAddress]
            ?: return BleConnectionResult.Failure(BleFailureReason.DEVICE_NOT_FOUND)

        if (device.connectionState == BleConnectionState.CONNECTED) {
            return BleConnectionResult.Failure(BleFailureReason.ALREADY_CONNECTED)
        }

        // Update state to CONNECTING
        updateDeviceState(macAddress, BleConnectionState.CONNECTING)

        // Delegate actual GATT connection to existing BluetoothTransportImpl
        val result = bluetoothTransport.connect()
        return if (result is com.mesh.emergency.core.common.result.Result.Success) {
            updateDeviceState(macAddress, BleConnectionState.CONNECTED)

            // Bridge received bytes into our data stream
            scope.launch {
                bluetoothTransport.receive().collect { bytes ->
                    _dataStream.tryEmit(BleDataPacket(sourceDeviceId = macAddress, payload = bytes))
                }
            }

            BleConnectionResult.Success
        } else {
            updateDeviceState(macAddress, BleConnectionState.FAILED)
            BleConnectionResult.Failure(BleFailureReason.UNKNOWN_ERROR)
        }
    }

    override suspend fun disconnectDevice(macAddress: String) {
        bluetoothTransport.disconnect()
        updateDeviceState(macAddress, BleConnectionState.DISCONNECTED)
    }

    // ── A30.7 Data Channel ────────────────────────────────────────────────────

    override fun dataStream(): Flow<BleDataPacket> = _dataStream.asSharedFlow()

    // ─────────────────────────────────────────────────────────────────────────
    // Internals
    // ─────────────────────────────────────────────────────────────────────────

    private fun updateDeviceState(macAddress: String, newState: BleConnectionState) {
        deviceMap[macAddress]?.let { existing ->
            val updated = existing.copy(connectionState = newState, lastSeenMs = System.currentTimeMillis())
            deviceMap[macAddress] = updated
            _discoveredDevices.value = deviceMap.values.toList().sortedByDescending { it.rssi }
        }
    }

    fun destroy() {
        scope.cancel()
    }
}
