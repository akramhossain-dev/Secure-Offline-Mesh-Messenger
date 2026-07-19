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
import com.mesh.emergency.data.local.entity.DeviceEntity
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private val permissionManager: PermissionManager,
    private val localDataSource: com.mesh.emergency.data.local.LocalDataSource
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

    /**
     * Pending reverse-handshake payload waiting for the next BLE connection event.
     * Drained automatically when a GATT client connects to our server OR when we
     * discover services on a remote GATT server as a client.
     */
    @Volatile
    private var pendingReverseHandshake: ByteArray? = null

    private val scope = CoroutineScope(Dispatchers.IO)

    // GATT Service and Characteristic UUIDs for Emergency Mesh
    private val SERVICE_UUID = UUID.fromString("0000FF10-0000-1000-8000-00805F9B34FB")
    private val RX_CHAR_UUID = UUID.fromString("0000FF11-0000-1000-8000-00805F9B34FB") // Client writes here
    private val TX_CHAR_UUID = UUID.fromString("0000FF12-0000-1000-8000-00805F9B34FB") // Server notifies client here
    private val CLIENT_CONFIG_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")

    /** Exposes the local Bluetooth MAC address for embedding in QR handshake payloads. */
    @get:SuppressLint("HardwareIds")
    val localBleAddress: String get() = try { adapter?.address ?: "" } catch (e: Exception) { "" }

    /**
     * Delivers [payload] to all BLE devices that are currently connected as GATT clients
     * to our server via [notifyCharacteristicChanged].
     *
     * This is the **primary** reverse-handshake channel because:
     * - It reuses already-established connections — no new [connectGatt] needed.
     * - It does NOT depend on the scanner's BLE MAC address (which is anonymised
     *   as 02:00:00:00:00:00 on Android 10+ and is therefore unreliable as a target).
     *
     * @return true if at least one client was notified successfully.
     */
    @SuppressLint("MissingPermission")
    fun sendViaConnectedClients(payload: ByteArray): Boolean {
        val activeServer = gattServer ?: run {
            Timber.w("BluetoothTransport: sendViaConnectedClients — GATT server not running")
            return false
        }
        val serverChar = txServerCharacteristic ?: run {
            Timber.w("BluetoothTransport: sendViaConnectedClients — TX characteristic not initialised")
            return false
        }
        if (connectedDevices.isEmpty()) {
            Timber.d("BluetoothTransport: sendViaConnectedClients — no clients connected yet")
            return false
        }
        serverChar.value = payload
        var successCount = 0
        for (device in connectedDevices) {
            val ok = activeServer.notifyCharacteristicChanged(device, serverChar, false)
            if (ok) successCount++
            Timber.d("BluetoothTransport: notifyCharacteristicChanged → ${device.address} ok=$ok")
        }
        Timber.d("BluetoothTransport: sendViaConnectedClients notified $successCount/${connectedDevices.size} clients")
        return successCount > 0
    }

    /**
     * Sends [payload] via the EXISTING GATT client connection that was established
     * during startup scanning (Phone A connected as a client to Phone B's server).
     *
     * This is the most reliable path when the phones have already connected during
     * the BLE discovery phase — no MAC address needed, connection already live.
     *
     * @return true if the write was queued successfully.
     */
    @SuppressLint("MissingPermission")
    fun sendViaGattClient(payload: ByteArray): Boolean {
        val activeGatt = gatt ?: run {
            Timber.d("BluetoothTransport: sendViaGattClient — no GATT client connection active")
            return false
        }
        val char = rxCharacteristic ?: run {
            Timber.d("BluetoothTransport: sendViaGattClient — RX characteristic not yet discovered")
            return false
        }
        char.value = payload
        char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        val queued = activeGatt.writeCharacteristic(char)
        Timber.d("BluetoothTransport: sendViaGattClient write queued=$queued")
        return queued
    }

    /**
     * Three-path reverse handshake delivery with pending queue + reconnect fallback.
     *
     * Priority order:
     *  1. [sendViaConnectedClients] — notify GATT clients connected to OUR server.
     *  2. [sendViaGattClient]       — write to the server WE are connected to as a client.
     *  3. [sendReverseHandshake]    — open a new GATT connection to [targetMac] (only if real MAC).
     *  4. Pending queue + scan      — store payload and immediately trigger a BLE reconnect scan.
     *
     * Because Android 10+ returns `02:00:00:00:00:00` for `adapter.address`, the QR
     * payload's `ble` field is often unusable for path 3.  Paths 1 and 2 work without
     * any MAC address by reusing already-established connections.
     */
    suspend fun queueAndDeliverReverseHandshake(payload: ByteArray, targetMac: String) {
        // Path 1: server notify (Phone A is server, Phone B is a connected client)
        val serverSent = sendViaConnectedClients(payload)
        Timber.d("BLE_FLOW: queueAndDeliver — path1 serverNotify=$serverSent")

        // Path 2: client write (Phone A is a GATT client already connected to Phone B's server)
        val clientSent = if (!serverSent) sendViaGattClient(payload) else false
        Timber.d("BLE_FLOW: queueAndDeliver — path2 clientWrite=$clientSent")

        if (serverSent || clientSent) {
            pendingReverseHandshake = null  // delivered, clear any stale pending
            return
        }

        // Path 3: direct GATT connect to the scanned MAC (only if the address looks real)
        if (targetMac.isNotBlank() && targetMac != "02:00:00:00:00:00") {
            Timber.d("BLE_FLOW: queueAndDeliver — path3 directGATT to $targetMac")
            sendReverseHandshake(targetMac, payload)
        } else {
            // Path 4: BLE is offline — queue the payload and immediately trigger a reconnect scan.
            // The pending payload will be drained by drainPendingViaClient() / drainPendingViaServer()
            // as soon as any GATT connection is established by the scan.
            Timber.w("BLE_FLOW: queueAndDeliver — BLE offline. Queuing pending handshake + triggering reconnect scan")
            pendingReverseHandshake = payload
            scope.launch { triggerReconnectScan() }
        }
    }

    /** Drains [pendingReverseHandshake] if one is queued, using the server notify path. */
    private fun drainPendingViaServer() {
        val pending = pendingReverseHandshake ?: return
        Timber.d("BLE_FLOW: Draining pending reverse handshake via server notify")
        val sent = sendViaConnectedClients(pending)
        if (sent) {
            pendingReverseHandshake = null
            Timber.d("PAIR_FLOW: PAIR_REQUEST_SENT (drained via server notify)")
        }
    }

    /** Drains [pendingReverseHandshake] if one is queued, using the GATT client write path. */
    @SuppressLint("MissingPermission")
    private fun drainPendingViaClient() {
        val pending = pendingReverseHandshake ?: return
        Timber.d("BLE_FLOW: Draining pending reverse handshake via GATT client write")
        val sent = sendViaGattClient(pending)
        if (sent) {
            pendingReverseHandshake = null
            Timber.d("PAIR_FLOW: PAIR_REQUEST_SENT (drained via client write)")
        }
    }

    /**
     * Performs a targeted BLE scan for any peer advertising our SERVICE_UUID and opens
     * a GATT client connection to the first one found.  This is triggered by [queueAndDeliverReverseHandshake]
     * when BLE is offline at the moment of QR scan so the pending handshake gets delivered
     * as soon as the GATT connection is established (via [drainPendingViaClient] in
     * [onServicesDiscovered]).
     *
     * Also restarts the GATT server and advertising if they were stopped.
     */
    @SuppressLint("MissingPermission")
    private suspend fun triggerReconnectScan() {
        if (!permissionManager.hasPermission(context, PermissionType.BLUETOOTH)) return
        val activeAdapter = adapter ?: return
        if (!activeAdapter.isEnabled) return

        Timber.d("BLE_FLOW: triggerReconnectScan — BLE scanning started (QR-triggered reconnect)")

        // Ensure GATT server + advertising are running (they may have been stopped or never started)
        if (gattServer == null) {
            Timber.d("BLE_FLOW: GATT server not running — restarting")
            startGattServer()
            startAdvertising()
            Timber.d("BLE_FLOW: BLE advertising started")
        }

        val scanner = activeAdapter.bluetoothLeScanner ?: run {
            Timber.e("BLE_FLOW: BLE scanner not available")
            return
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        val filters = listOf(
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(SERVICE_UUID))
                .build()
        )

        var foundDevice: BluetoothDevice? = null
        val scanCb = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                if (foundDevice == null) {
                    foundDevice = result?.device
                    Timber.d("BLE_FLOW: Device discovered during reconnect scan: ${result?.device?.address}")
                }
            }
            override fun onScanFailed(errorCode: Int) {
                Timber.e("BLE_FLOW: Connection failed — BLE scan error code: $errorCode")
            }
        }

        withContext(Dispatchers.Main) { scanner.startScan(filters, settings, scanCb) }

        // Wait up to 15 seconds for a device to appear
        var waited = 0
        while (foundDevice == null && waited < 15000) {
            delay(200L)
            waited += 200
        }

        withContext(Dispatchers.Main) { scanner.stopScan(scanCb) }

        val device = foundDevice
        if (device != null) {
            Timber.d("BLE_FLOW: GATT connection created to ${device.address} (reconnect scan)")
            withContext(Dispatchers.Main) {
                // Only create a new GATT client if we don't already have one
                if (gatt == null) {
                    gatt = device.connectGatt(context, false, gattCallback)
                }
            }
        } else {
            Timber.w("BLE_FLOW: Connection failed — no peer found in 15s reconnect scan")
        }
    }

    /**
     * Connects to [targetMac] via a temporary GATT client and writes [payload] to the
     * remote device's RX characteristic.  Used as the **fallback** reverse-handshake
     * channel when [sendViaConnectedClients] finds no connected clients.
     *
     * NOTE: On Android 10+ `adapter.address` returns the anonymised placeholder
     * `02:00:00:00:00:00`, so if [targetMac] has that value this call is a no-op.
     * The write race condition is fixed by only disconnecting inside [onCharacteristicWrite].
     */
    @SuppressLint("MissingPermission")
    suspend fun sendReverseHandshake(targetMac: String, payload: ByteArray) {
        if (targetMac.isBlank() || targetMac == "02:00:00:00:00:00") {
            Timber.w("BluetoothTransport: sendReverseHandshake — skipping bogus/blank MAC '$targetMac'")
            return
        }
        val activeAdapter = adapter ?: return
        Timber.d("BluetoothTransport: sendReverseHandshake → $targetMac (${payload.size} bytes)")

        val callback = object : BluetoothGattCallback() {
            @SuppressLint("MissingPermission")
            override fun onConnectionStateChange(g: BluetoothGatt?, status: Int, newState: Int) {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        Timber.d("BluetoothTransport: Reverse handshake GATT connected to $targetMac")
                        g?.discoverServices()
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        g?.close()
                        Timber.d("BluetoothTransport: Reverse handshake GATT disconnected from $targetMac")
                    }
                }
            }

            @SuppressLint("MissingPermission")
            override fun onServicesDiscovered(g: BluetoothGatt?, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    val char = g?.getService(SERVICE_UUID)?.getCharacteristic(RX_CHAR_UUID)
                    if (char != null) {
                        char.value = payload
                        char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                        val queued = g.writeCharacteristic(char)
                        Timber.d("BluetoothTransport: Reverse handshake write queued=$queued to $targetMac")
                        if (!queued) {
                            // Write couldn't be queued — disconnect immediately
                            scope.launch { delay(200L); g.disconnect() }
                        }
                        // Disconnect is deferred to onCharacteristicWrite to avoid race condition
                    } else {
                        Timber.w("BluetoothTransport: RX characteristic not found on $targetMac")
                        scope.launch { delay(200L); g?.disconnect() }
                    }
                } else {
                    Timber.w("BluetoothTransport: Service discovery failed on $targetMac status=$status")
                    g?.disconnect()
                }
            }

            @SuppressLint("MissingPermission")
            override fun onCharacteristicWrite(
                g: BluetoothGatt?,
                characteristic: BluetoothGattCharacteristic?,
                status: Int
            ) {
                // Only disconnect AFTER the write has completed — fixes the race condition
                val ok = status == BluetoothGatt.GATT_SUCCESS
                Timber.d("BluetoothTransport: Reverse handshake write confirmed on $targetMac ok=$ok")
                scope.launch { delay(300L); g?.disconnect() }
            }
        }

        withContext(Dispatchers.Main) {
            activeAdapter.getRemoteDevice(targetMac).connectGatt(context, false, callback)
        }
    }


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
        Timber.d("BLE_FLOW: BLE advertising started")

        // 2. Try direct GATT connection to already-paired devices (via stored BLE MAC)
        val pairedDevices: List<DeviceEntity> = try {
            localDataSource.getDevices().first()
        } catch (e: Exception) { emptyList() }

        val knownMacs: List<String> = pairedDevices.mapNotNull { d ->
            d.bleAddress.takeIf { it.isNotBlank() && it != "02:00:00:00:00:00" }
        }

        if (knownMacs.isNotEmpty()) {
            Timber.d("BLE_FLOW: Attempting direct connection to ${knownMacs.size} paired device(s): $knownMacs")
            for (mac in knownMacs) {
                try {
                    val device = activeAdapter.getRemoteDevice(mac)
                    Timber.d("BLE_FLOW: GATT connection created to $mac (direct paired)")
                    withContext(Dispatchers.Main) {
                        gatt = device.connectGatt(context, false, gattCallback)
                    }
                    // Wait up to 8s for connection to establish
                    var waited = 0
                    while (_status.value != TransportStatus.CONNECTED && waited < 8000) {
                        delay(200L)
                        waited += 200
                    }
                    if (_status.value == TransportStatus.CONNECTED) {
                        Timber.d("BLE_FLOW: Direct GATT connection established to $mac")
                        return Result.Success(Unit)
                    } else {
                        Timber.d("BLE_FLOW: Direct connect to $mac timed out, trying next...")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "BLE_FLOW: Connection failed for $mac")
                }
            }
        }

        // 3. Fallback: Persistent BLE scanning with automatic retry.
        //    Runs in a background loop so the app reconnects automatically without user action.
        //    Each scan window is 12 seconds; on timeout it waits 20 seconds then retries.
        scope.launch {
            Timber.d("BLE_FLOW: BLE scanning started (persistent loop)")
            while (true) {
                if (_status.value == TransportStatus.CONNECTED) {
                    delay(10000L)   // already connected — check again in 10s
                    continue
                }
                val scanner = activeAdapter.bluetoothLeScanner ?: break

                var foundDevice: BluetoothDevice? = null
                val scanCallback = object : ScanCallback() {
                    override fun onScanResult(callbackType: Int, result: ScanResult?) {
                        result?.let { res ->
                            val uuids = res.scanRecord?.serviceUuids
                            if (uuids != null && uuids.contains(ParcelUuid(SERVICE_UUID))) {
                                if (foundDevice == null) {
                                    foundDevice = res.device
                                    Timber.d("BLE_FLOW: Device discovered: ${res.device.address}")
                                }
                            }
                        }
                    }
                    override fun onScanFailed(errorCode: Int) {
                        Timber.e("BLE_FLOW: Connection failed — BLE scan error code: $errorCode")
                    }
                }

                val settings = ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build()
                val filters = listOf(
                    ScanFilter.Builder()
                        .setServiceUuid(ParcelUuid(SERVICE_UUID))
                        .build()
                )

                withContext(Dispatchers.Main) { scanner.startScan(filters, settings, scanCallback) }

                // Wait up to 12 seconds for a device to appear
                var waited = 0
                while (foundDevice == null && waited < 12000) {
                    delay(200L)
                    waited += 200
                }

                withContext(Dispatchers.Main) { scanner.stopScan(scanCallback) }

                val target = foundDevice
                if (target != null) {
                    Timber.d("BLE_FLOW: GATT connection created to ${target.address}")
                    withContext(Dispatchers.Main) {
                        if (gatt == null) {
                            gatt = target.connectGatt(context, false, gattCallback)
                        }
                    }
                    // Wait for connection to establish before scanning again
                    delay(8000L)
                } else {
                    Timber.d("BLE_FLOW: Scan window complete — no peer found. Retrying in 20s")
                    // Update status: if we have server clients we're still connected, else offline
                    if (_status.value != TransportStatus.CONNECTED) {
                        _status.value = if (connectedDevices.isNotEmpty()) {
                            TransportStatus.CONNECTED
                        } else {
                            TransportStatus.DISCONNECTED
                        }
                    }
                    delay(20000L)  // wait 20s before next scan cycle
                }
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
            .setIncludeDeviceName(false)
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
                    // Drain any pending reverse handshake now that a client is connected
                    scope.launch { drainPendingViaServer() }
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
                val str = String(value, Charsets.UTF_8)
                Timber.d("BLE_FLOW: Data characteristic received — ${str.take(120)}")

                // Check if this is a reverse handshake — auto-save the sender as a paired contact
                if (str.contains("\"type\":\"REVERSE_HANDSHAKE\"")) {
                    try {
                        val json = org.json.JSONObject(str)
                        val remoteUserId    = json.getString("uid")
                        val remoteUsername  = json.optString("un", "").ifBlank { "Contact-${remoteUserId.take(6)}" }
                        val remotePublicKey = json.optString("pub", "")
                        val realBleAddress  = device?.address?.takeIf { it != "02:00:00:00:00:00" }
                            ?: json.optString("ble", "")
                        val remoteDeviceType = json.optString("dt", "SMARTPHONE")

                        Timber.d("PAIR_FLOW: Pair request received (via server RX write) — uid=$remoteUserId un='$remoteUsername' ble=$realBleAddress")

                        scope.launch {
                            val userEntity = com.mesh.emergency.data.local.entity.UserEntity(
                                entityId = remoteUserId,
                                username = remoteUsername,
                                profileImageRef = null,
                                languagePreference = "en",
                                createdTime = System.currentTimeMillis(),
                                updatedTime = System.currentTimeMillis(),
                                status = "ACTIVE",
                                isCurrentUser = false,
                                lastSeen = System.currentTimeMillis(),
                                trustedStatus = true,
                                nickname = remoteUsername,
                                publicKey = remotePublicKey
                            )
                            val deviceEntity = com.mesh.emergency.data.local.entity.DeviceEntity(
                                entityId = remoteUserId,
                                name = remoteUsername,
                                rssi = -55,
                                lastSeen = System.currentTimeMillis(),
                                deviceType = remoteDeviceType,
                                platformInfo = "ANDROID",
                                createdTime = System.currentTimeMillis(),
                                lastActiveTime = System.currentTimeMillis(),
                                trustStatus = com.mesh.emergency.data.local.entity.DbTrustStatus.TRUSTED,
                                nickname = remoteUsername,
                                bleAddress = realBleAddress
                            )
                            Timber.d("DATABASE: Before saving peer (server RX) — userId=$remoteUserId name='$remoteUsername'")
                            localDataSource.insertUser(userEntity)
                            Timber.d("DATABASE: After saving peer — UserEntity inserted id=$remoteUserId name='$remoteUsername'")
                            localDataSource.insertDevice(deviceEntity)
                            Timber.d("DATABASE: After saving peer — DeviceEntity inserted id=$remoteUserId ble=$realBleAddress")
                            Timber.d("PAIR_FLOW: Pair accepted — bidirectional pairing complete for $remoteUserId")
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "PAIR_FLOW: Failed to parse reverse handshake (server RX)")
                        _receiveFlow.tryEmit(value)
                    }
                } else {
                    _receiveFlow.tryEmit(value)
                }

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
                    // Request larger MTU FIRST — default is 23 bytes (20 bytes payload), which is
                    // far too small for the REVERSE_HANDSHAKE JSON (~200 bytes). Without this,
                    // the payload is silently truncated and the contains("REVERSE_HANDSHAKE")
                    // check fails. We discover services only after MTU is negotiated.
                    Timber.d("BLE_FLOW: GATT connected — requesting MTU 512 before service discovery")
                    gatt?.requestMtu(512)
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    _status.value = if (connectedDevices.isNotEmpty()) TransportStatus.CONNECTED else TransportStatus.DISCONNECTED
                    this@BluetoothTransportImpl.gatt = null
                    rxCharacteristic = null
                    Timber.d("BLE_FLOW: Connection failed — GATT client disconnected from server")
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            Timber.d("BLE_FLOW: MTU negotiated to $mtu bytes (payload capacity: ${mtu - 3} bytes) status=$status")
            // Discover services now that we have a workable MTU.
            // Even if MTU negotiation failed (status != GATT_SUCCESS), we still proceed
            // because the server may have accepted a partial MTU upgrade.
            gatt?.discoverServices()
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
                    // Drain any pending reverse handshake now that the GATT client connection is ready
                    drainPendingViaClient()
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
                    val str = String(data, Charsets.UTF_8)
                    Timber.d("BLE_FLOW: Data characteristic received (client TX notification) — ${str.take(80)}")

                    // Handle REVERSE_HANDSHAKE delivered via server notify (TX characteristic)
                    if (str.contains("\"type\":\"REVERSE_HANDSHAKE\"")) {
                        Timber.d("PAIR_FLOW: Pair request received (via client TX notification)")
                        try {
                            val json = org.json.JSONObject(str)
                            val remoteUserId    = json.getString("uid")
                            val remoteUsername  = json.optString("un", "").ifBlank { "Contact-${remoteUserId.take(6)}" }
                            val remotePublicKey = json.optString("pub", "")
                            val realBleAddress  = gatt?.device?.address?.takeIf { it != "02:00:00:00:00:00" }
                                ?: json.optString("ble", "")
                            val remoteDeviceType = json.optString("dt", "SMARTPHONE")

                            Timber.d("PAIR_FLOW: Pair request received — uid=$remoteUserId un='$remoteUsername' ble=$realBleAddress")
                            scope.launch {
                                val userEntity = com.mesh.emergency.data.local.entity.UserEntity(
                                    entityId = remoteUserId,
                                    username = remoteUsername,
                                    profileImageRef = null,
                                    languagePreference = "en",
                                    createdTime = System.currentTimeMillis(),
                                    updatedTime = System.currentTimeMillis(),
                                    status = "ACTIVE",
                                    isCurrentUser = false,
                                    lastSeen = System.currentTimeMillis(),
                                    trustedStatus = true,
                                    nickname = remoteUsername,
                                    publicKey = remotePublicKey
                                )
                                val deviceEntity = com.mesh.emergency.data.local.entity.DeviceEntity(
                                    entityId = remoteUserId,
                                    name = remoteUsername,
                                    rssi = -55,
                                    lastSeen = System.currentTimeMillis(),
                                    deviceType = remoteDeviceType,
                                    platformInfo = "ANDROID",
                                    createdTime = System.currentTimeMillis(),
                                    lastActiveTime = System.currentTimeMillis(),
                                    trustStatus = com.mesh.emergency.data.local.entity.DbTrustStatus.TRUSTED,
                                    nickname = remoteUsername,
                                    bleAddress = realBleAddress
                                )
                                Timber.d("DATABASE: Before saving peer (client TX) — userId=$remoteUserId name='$remoteUsername'")
                                localDataSource.insertUser(userEntity)
                                Timber.d("DATABASE: After saving peer — UserEntity inserted id=$remoteUserId name='$remoteUsername'")
                                localDataSource.insertDevice(deviceEntity)
                                Timber.d("DATABASE: After saving peer — DeviceEntity inserted id=$remoteUserId ble=$realBleAddress")
                                Timber.d("PAIR_FLOW: Pair accepted — bidirectional pairing complete for $remoteUserId")
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "PAIR_FLOW: Failed to handle REVERSE_HANDSHAKE notification (client TX)")
                            _receiveFlow.tryEmit(data)
                        }
                    } else {
                        _receiveFlow.tryEmit(data)
                    }
                }
            }
        }
    }
}
