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
import com.mesh.emergency.data.local.entity.NetworkNodeEntity
import com.mesh.emergency.data.local.entity.DbNodeStatus
import com.mesh.emergency.data.local.entity.DbNodeType
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
import kotlinx.coroutines.flow.firstOrNull
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
    private val localDataSource: com.mesh.emergency.data.local.LocalDataSource,
    private val messageNotifier: com.mesh.emergency.core.notification.MessageNotifier,
    private val notificationManager: com.mesh.emergency.core.notification.NotificationManager
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

    private val seenPacketIds = java.util.Collections.synchronizedSet(object : java.util.LinkedHashSet<String>() {
        override fun add(element: String): Boolean {
            if (size > 1000) {
                val iterator = iterator()
                if (iterator.hasNext()) {
                    iterator.next()
                    iterator.remove()
                }
            }
            return super.add(element)
        }
    })

    /**
     * Pending reverse-handshake payload waiting for the next BLE connection event.
     * Drained automatically when a GATT client connects to our server OR when we
     * discover services on a remote GATT server as a client.
     */
    @Volatile
    private var pendingReverseHandshake: ByteArray? = null

    private val scope = CoroutineScope(Dispatchers.IO)

    private val _packetsSentCount = MutableStateFlow(0)
    val packetsSentCount: StateFlow<Int> = _packetsSentCount.asStateFlow()

    private val _packetsReceivedCount = MutableStateFlow(0)
    val packetsReceivedCount: StateFlow<Int> = _packetsReceivedCount.asStateFlow()

    private val _failedPacketsCount = MutableStateFlow(0)
    val failedPacketsCount: StateFlow<Int> = _failedPacketsCount.asStateFlow()

    private var connectionStartTime = 0L

    fun getConnectionUptime(): Long {
        return if (connectionStartTime == 0L) 0L else System.currentTimeMillis() - connectionStartTime
    }

    private fun updateStatus(newStatus: TransportStatus) {
        _status.value = newStatus
        if (newStatus == TransportStatus.CONNECTED) {
            if (connectionStartTime == 0L) {
                connectionStartTime = System.currentTimeMillis()
                Timber.d("BLE_FLOW: Connection established, starting uptime timer")
            }
        } else if (newStatus == TransportStatus.DISCONNECTED || newStatus == TransportStatus.UNAVAILABLE) {
            if (connectedDevices.isEmpty() && gatt == null) {
                connectionStartTime = 0L
                Timber.d("BLE_FLOW: All connections lost, resetting uptime timer")
            }
        }
    }

    init {
        startRssiPollLoop()
        scope.launch {
            try {
                // Delete all nodes from the 'network_nodes' table on startup to clear old session stale/corrupt data
                val existingNodes = localDataSource.getNetworkNodes().firstOrNull() ?: emptyList()
                existingNodes.forEach { node ->
                    localDataSource.deleteNode(node)
                    Timber.d("BLE_FLOW: Cleared stale node on startup: nodeId=${node.entityId}")
                }
                
                // Also clean up any corrupt devices in the 'devices' table (where entityId is a MAC address containing colons)
                val existingDevices = localDataSource.getDevices().firstOrNull() ?: emptyList()
                existingDevices.forEach { device ->
                    if (device.entityId.contains(":")) {
                        localDataSource.deleteDevice(device)
                        Timber.d("BLE_FLOW: Cleared corrupt device entity on startup: deviceId=${device.entityId}")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "BLE_FLOW: Failed to run startup DB cleanup")
            }
        }
    }

    private fun startRssiPollLoop() {
        scope.launch {
            while (true) {
                delay(5000L)
                val activeGatt = gatt
                if (activeGatt != null && _status.value == TransportStatus.CONNECTED) {
                    try {
                        Timber.d("BLE_FLOW: Polling remote RSSI...")
                        activeGatt.readRemoteRssi()
                    } catch (e: SecurityException) {
                        Timber.e(e, "BLE_FLOW: Security exception reading RSSI")
                    } catch (e: Exception) {
                        Timber.e(e, "BLE_FLOW: Failed to read RSSI")
                    }
                }
            }
        }
    }

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
        if (successCount > 0) {
            _packetsSentCount.value += successCount
        } else {
            _failedPacketsCount.value++
        }
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
        if (queued) {
            _packetsSentCount.value++
        } else {
            _failedPacketsCount.value++
        }
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
                result?.let { res ->
                    if (foundDevice == null) {
                        foundDevice = res.device
                        Timber.d("BLE_FLOW: Device discovered during reconnect scan: address=${res.device.address} name=${res.device.name} rssi=${res.rssi}")
                    }
                    val address = res.device.address
                    val name = res.device.name ?: "Node-${address.take(6)}"
                    val rssi = res.rssi
                    scope.launch {
                        updateNodeInDb(address, name, rssi, DbNodeStatus.ONLINE)
                    }
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
                        if (queued) {
                            _packetsSentCount.value++
                        } else {
                            _failedPacketsCount.value++
                            // Write couldn't be queued — disconnect immediately
                            scope.launch { delay(200L); g.disconnect() }
                        }
                        // Disconnect is deferred to onCharacteristicWrite to avoid race condition
                    } else {
                        _failedPacketsCount.value++
                        Timber.w("BluetoothTransport: RX characteristic not found on $targetMac")
                        scope.launch { delay(200L); g?.disconnect() }
                    }
                } else {
                    _failedPacketsCount.value++
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
                if (!ok) {
                    _failedPacketsCount.value++
                }
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

        updateStatus(TransportStatus.CONNECTING)

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
                    override fun onScanResult(callbackType: Int, res: ScanResult?) {
                        if (res == null) return
                        if (foundDevice == null) {
                            foundDevice = res.device
                            Timber.d("BLE_FLOW: Device discovered during scan window: address=${res.device.address} name=${res.device.name} rssi=${res.rssi}")
                        }
                        val address = res.device.address
                        val name = res.device.name ?: "Node-${address.take(6)}"
                        val rssi = res.rssi
                        scope.launch {
                            updateNodeInDb(address, name, rssi, DbNodeStatus.ONLINE)
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
                        updateStatus(
                            if (connectedDevices.isNotEmpty()) TransportStatus.CONNECTED
                            else TransportStatus.DISCONNECTED
                        )
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

        updateStatus(TransportStatus.DISCONNECTED)
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
                _packetsSentCount.value++
                Timber.d("BLE_FLOW: Packet sent successfully as client (length=${data.size})")
                return Result.Success(Unit)
            } else {
                _failedPacketsCount.value++
                Timber.e("BLE_FLOW: Failed to send packet as client")
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
                _packetsSentCount.value += successCount
                Timber.d("BLE_FLOW: Packet notified to $successCount server clients (length=${data.size})")
                return Result.Success(Unit)
            } else {
                _failedPacketsCount.value++
                Timber.e("BLE_FLOW: Failed to notify any server clients")
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
                    updateStatus(TransportStatus.CONNECTED)
                    Timber.d("BLE_FLOW: Connected (Server Role) — GATT client connected: address=${device.address} name=${device.name}")
                    scope.launch {
                        updateNodeInDb(device.address, device.name ?: "Node-${device.address.take(6)}", -50, DbNodeStatus.ONLINE)
                        // Drain any QR-triggered pending reverse handshake first
                        drainPendingViaServer()
                        // Push our identity to the new client so they can save us as a peer.
                        // Delay 800 ms: let the client enable TX notifications before we notify.
                        delay(800L)
                        val identityPayload = buildLocalIdentityPayload()
                        if (identityPayload != null) {
                            val sent = sendViaConnectedClients(identityPayload)
                            Timber.d("PAIR_FLOW: Identity pushed to new GATT client — sent=$sent (${identityPayload.size} bytes)")
                        } else {
                            Timber.w("PAIR_FLOW: Skipping identity push — local profile not set up yet")
                        }
                    }
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    connectedDevices.remove(device)
                    updateStatus(if (connectedDevices.isNotEmpty() || gatt != null) TransportStatus.CONNECTED else TransportStatus.DISCONNECTED)
                    Timber.d("BLE_FLOW: Disconnected (Server Role) — GATT client disconnected: address=${device.address}")
                    scope.launch {
                        updateNodeInDb(device.address, device.name ?: "Node-${device.address.take(6)}", -100, DbNodeStatus.OFFLINE)
                    }
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
                _packetsReceivedCount.value++
                val str = String(value, Charsets.UTF_8)
                Timber.d("BLE_FLOW: Data characteristic received (server RX, length=${value.size}) — ${str.take(120)}")

                when {
                    // ── Global Chat broadcast packet ────────────────────────────
                    str.startsWith("GCHAT:") -> {
                        scope.launch {
                            handleInboundGlobalMessage(str.removePrefix("GCHAT:"), senderDeviceAddress = device?.address)
                        }
                    }

                    // ── Chat message packet ──────────────────────────────────
                    str.startsWith("MSG:") -> {
                        scope.launch {
                            handleInboundMessage(str.removePrefix("MSG:"), device?.address)
                        }
                    }

                    // ── Auto-identity exchange (server RX) ───────────────────
                    // Phone B connected as GATT client and wrote its identity to our RX char
                    str.startsWith("IDENTITY:") -> {
                        Timber.d("PAIR_FLOW: IDENTITY packet received via server RX write")
                        scope.launch { handlePeerIdentity(str.removePrefix("IDENTITY:"), device?.address) }
                    }

                    // ── Reverse handshake (pairing) ──────────────────────────
                    str.contains("\"type\":\"REVERSE_HANDSHAKE\"") -> {
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
                    }

                    // ── Unknown payload — forward to receive flow ────────────
                    else -> _receiveFlow.tryEmit(value)
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
                    Timber.d("BLE_FLOW: Connected (Client Role) — GATT connected to server: address=${gatt?.device?.address} name=${gatt?.device?.name}")
                    updateStatus(TransportStatus.CONNECTED)
                    gatt?.requestMtu(512)
                    gatt?.device?.let { dev ->
                        val address = dev.address
                        val name = dev.name ?: "Node-${address.take(6)}"
                        scope.launch {
                            updateNodeInDb(address, name, -50, DbNodeStatus.ONLINE)
                        }
                    }
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    updateStatus(if (connectedDevices.isNotEmpty()) TransportStatus.CONNECTED else TransportStatus.DISCONNECTED)
                    val disconnectedDevice = gatt?.device
                    this@BluetoothTransportImpl.gatt = null
                    rxCharacteristic = null
                    Timber.d("BLE_FLOW: Disconnected (Client Role) — GATT client disconnected from server: address=${disconnectedDevice?.address}")
                    disconnectedDevice?.let { dev ->
                        scope.launch {
                            updateNodeInDb(dev.address, dev.name ?: "Node-${dev.address.take(6)}", -100, DbNodeStatus.OFFLINE)
                        }
                    }
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

                Timber.d("BLE_FLOW: Services discovered on ${gatt?.device?.address} — rxChar=${rxCharacteristic != null} txChar=${txChar != null}")

                // Enable GATT Notifications on the Tx Characteristic so the server can notify us
                if (txChar != null) {
                    gatt.setCharacteristicNotification(txChar, true)
                    val descriptor = txChar.getDescriptor(CLIENT_CONFIG_DESCRIPTOR_UUID)
                    if (descriptor != null) {
                        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        val ok = gatt.writeDescriptor(descriptor)
                        // ⚠️  GATT only allows ONE outstanding operation at a time.
                        // Drain + identity write is deferred to onDescriptorWrite so we don't
                        // collide with the descriptor write that is still in-flight here.
                        Timber.d("BLE_FLOW: CCCD descriptor write queued=$ok — drain deferred to onDescriptorWrite")
                    } else {
                        // No CCCD — safe to proceed immediately
                        updateStatus(TransportStatus.CONNECTED)
                        scope.launch { drainPendingViaClient(); sendIdentityAsClient() }
                    }
                } else {
                    updateStatus(if (connectedDevices.isNotEmpty()) TransportStatus.CONNECTED else TransportStatus.DISCONNECTED)
                    // Still try to send our identity even without TX notifications
                    scope.launch { drainPendingViaClient(); sendIdentityAsClient() }
                }
            } else {
                Timber.w("BLE_FLOW: Service discovery failed status=$status")
                updateStatus(if (connectedDevices.isNotEmpty()) TransportStatus.CONNECTED else TransportStatus.DISCONNECTED)
            }
        }

        /**
         * Called when the CCCD descriptor write completes — at this point TX notifications
         * are enabled and the GATT channel is idle, so we can safely write our identity
         * and drain any pending reverse handshake without a GATT operation collision.
         */
        @SuppressLint("MissingPermission")
        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
            if (descriptor?.uuid == CLIENT_CONFIG_DESCRIPTOR_UUID) {
                updateStatus(TransportStatus.CONNECTED)
                Timber.d("BLE_FLOW: TX notifications enabled (descriptor write status=$status) — draining + sending identity")
                scope.launch {
                    // Drain any QR-triggered pending reverse handshake first
                    drainPendingViaClient()
                    // Then send our identity so the server can add us as a paired peer
                    sendIdentityAsClient()
                }
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            if (characteristic?.uuid == TX_CHAR_UUID) {
                val data = characteristic.value
                if (data != null) {
                    _packetsReceivedCount.value++
                    val str = String(data, Charsets.UTF_8)
                    Timber.d("BLE_FLOW: Data characteristic received (client TX notification, length=${data.size}) — ${str.take(80)}")

                    when {
                        // ── Global Chat broadcast packet ───────────────────────────
                        str.startsWith("GCHAT:") -> {
                            scope.launch {
                                handleInboundGlobalMessage(str.removePrefix("GCHAT:"), senderDeviceAddress = gatt?.device?.address)
                            }
                        }

                        // ── Chat message packet ────────────────────────────────────────
                        str.startsWith("MSG:") -> {
                            scope.launch {
                                handleInboundMessage(str.removePrefix("MSG:"), gatt?.device?.address)
                            }
                        }

                        // ── Auto-identity exchange (client RX notification) ──
                        // Server pushed its identity to us via TX notify — save it and reply
                        str.startsWith("IDENTITY:") -> {
                            Timber.d("PAIR_FLOW: IDENTITY notification received from server ${gatt?.device?.address}")
                            scope.launch { handlePeerIdentity(str.removePrefix("IDENTITY:"), gatt?.device?.address) }
                        }

                        // ── Reverse handshake (pairing) ──────────────────────
                        str.contains("\"type\":\"REVERSE_HANDSHAKE\"") -> {
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
                        }

                        // ── Unknown payload — forward to receive flow ────────
                        else -> _receiveFlow.tryEmit(data)
                    }
                }
            }
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS && gatt != null) {
                val address = gatt.device.address
                val name = gatt.device.name ?: "Node-${address.take(6)}"
                scope.launch {
                    updateNodeInDb(address, name, rssi, DbNodeStatus.ONLINE)
                }
            }
        }
    }

    // ── Inbound GCHAT: global message handler ──────────────────────────────

    /**
     * Parses a `GCHAT:{json}` broadcast message received over BLE and persists it to Room
     * if this messageId has not been seen before (deduplication via DAO IGNORE conflict).
     *
     * Packet format: `{"id":"uuid","from":"senderId","name":"Alice","text":"Hello","ts":1234567890}`
     */
    private suspend fun handleInboundGlobalMessage(jsonStr: String, senderDeviceAddress: String?) {
        try {
            Timber.d("GCHAT_FLOW: Inbound global message received — ${jsonStr.take(120)}")
            val json       = org.json.JSONObject(jsonStr)
            val type       = json.optString("type", "chat")
            val msgId      = json.getString("id")
            val senderId   = json.getString("from")
            val senderName = json.optString("name", "Unknown")
            val timestamp  = json.optLong("ts", System.currentTimeMillis())

            // Check if we have already processed this packet to prevent infinite forwarding loops
            val actionKey = "gchat:${msgId}:${type}:${timestamp}"
            if (!seenPacketIds.add(actionKey)) {
                return
            }

            val existing = localDataSource.getGlobalMessageById(msgId)

            when (type) {
                "chat" -> {
                    if (existing == null) {
                        val text = json.getString("text")
                        val entity = com.mesh.emergency.data.local.entity.GlobalMessageEntity(
                            messageId      = msgId,
                            senderId       = senderId,
                            senderName     = senderName,
                            content        = text,
                            timestamp      = timestamp,
                            createdAt      = timestamp,
                            updatedAt      = timestamp,
                            edited         = false,
                            deleted        = false,
                            deliveryStatus = "DELIVERED",
                            readStatus     = "READ",
                            syncState      = "SYNCED",
                            editHistory    = emptyList()
                        )
                        localDataSource.insertGlobalMessage(entity)
                        
                        // Notify UI
                        messageNotifier.notifyGlobalMessage(msgId, senderId, senderName, text)
                        
                        // Play sound / notification
                        triggerInboundNotification(msgId, senderName, text, isGlobal = true)

                        // Mesh forwarding: re-broadcast
                        reBroadcastPacket("GCHAT:$jsonStr")
                    } else if (timestamp > existing.updatedAt) {
                        val updated = existing.copy(
                            content = json.getString("text"),
                            updatedAt = timestamp
                        )
                        localDataSource.insertGlobalMessage(updated)
                        reBroadcastPacket("GCHAT:$jsonStr")
                    }
                }
                "edit" -> {
                    val text = json.getString("text")
                    if (existing == null) {
                        val entity = com.mesh.emergency.data.local.entity.GlobalMessageEntity(
                            messageId      = msgId,
                            senderId       = senderId,
                            senderName     = senderName,
                            content        = text,
                            timestamp      = timestamp,
                            createdAt      = timestamp,
                            updatedAt      = timestamp,
                            edited         = true,
                            deleted        = false,
                            deliveryStatus = "DELIVERED",
                            readStatus     = "READ",
                            syncState      = "SYNCED",
                            editHistory    = emptyList()
                        )
                        localDataSource.insertGlobalMessage(entity)
                        reBroadcastPacket("GCHAT:$jsonStr")
                    } else if (timestamp > existing.updatedAt) {
                        val history = existing.editHistory.toMutableList()
                        if (!history.contains(existing.content)) {
                            history.add(existing.content)
                        }
                        val updated = existing.copy(
                            content = text,
                            edited = true,
                            updatedAt = timestamp,
                            editHistory = history
                        )
                        localDataSource.insertGlobalMessage(updated)
                        reBroadcastPacket("GCHAT:$jsonStr")
                    }
                }
                "delete" -> {
                    if (existing == null) {
                        val entity = com.mesh.emergency.data.local.entity.GlobalMessageEntity(
                            messageId      = msgId,
                            senderId       = senderId,
                            senderName     = senderName,
                            content        = "",
                            timestamp      = timestamp,
                            createdAt      = timestamp,
                            updatedAt      = timestamp,
                            edited         = false,
                            deleted        = true,
                            deliveryStatus = "DELIVERED",
                            readStatus     = "READ",
                            syncState      = "SYNCED",
                            editHistory    = emptyList()
                        )
                        localDataSource.insertGlobalMessage(entity)
                        reBroadcastPacket("GCHAT:$jsonStr")
                    } else if (timestamp > existing.updatedAt) {
                        val updated = existing.copy(
                            deleted = true,
                            updatedAt = timestamp
                        )
                        localDataSource.insertGlobalMessage(updated)
                        reBroadcastPacket("GCHAT:$jsonStr")
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "GCHAT_FLOW: Failed to parse inbound GCHAT: packet — $jsonStr")
        }
    }

    suspend fun sendGlobalMessage(
        messageId: String,
        senderId: String,
        senderName: String,
        text: String
    ): Boolean {
        val json = org.json.JSONObject().apply {
            put("type", "chat")
            put("id",   messageId)
            put("from", senderId)
            put("name", senderName)
            put("text", text)
            put("ts",   System.currentTimeMillis())
        }
        val payload = "GCHAT:$json".toByteArray(Charsets.UTF_8)
        Timber.d("GCHAT_FLOW: Sending global message — id=$messageId name='$senderName' text='${text.take(40)}'")
        return sendPayloadDirect(payload)
    }

    suspend fun sendGlobalMessageEdit(
        messageId: String,
        senderId: String,
        senderName: String,
        text: String
    ): Boolean {
        val json = org.json.JSONObject().apply {
            put("type", "edit")
            put("id",   messageId)
            put("from", senderId)
            put("name", senderName)
            put("text", text)
            put("ts",   System.currentTimeMillis())
        }
        val payload = "GCHAT:$json".toByteArray(Charsets.UTF_8)
        return sendPayloadDirect(payload)
    }

    suspend fun sendGlobalMessageDelete(
        messageId: String,
        senderId: String,
        senderName: String
    ): Boolean {
        val json = org.json.JSONObject().apply {
            put("type", "delete")
            put("id",   messageId)
            put("from", senderId)
            put("name", senderName)
            put("ts",   System.currentTimeMillis())
        }
        val payload = "GCHAT:$json".toByteArray(Charsets.UTF_8)
        return sendPayloadDirect(payload)
    }

    private suspend fun handleInboundMessage(jsonStr: String, senderBleAddress: String?) {
        try {
            Timber.d("MSG_FLOW: Inbound message received — ${jsonStr.take(120)}")
            val json       = org.json.JSONObject(jsonStr)
            val type       = json.optString("type", "chat")
            val msgId      = json.getString("id")
            val senderId   = json.getString("from")
            val receiverId = json.optString("to", "")
            val timestamp  = json.optLong("ts", System.currentTimeMillis())

            val currentUser = localDataSource.getCurrentUser().firstOrNull()
            val localUserId = currentUser?.entityId ?: "self"

            if (receiverId == localUserId) {
                val actionKey = "msg:${msgId}:${type}:${timestamp}"
                if (!seenPacketIds.add(actionKey)) {
                    return
                }

                val senderUser = localDataSource.getUserById(senderId)
                val senderName = senderUser?.nickname?.takeIf { it.isNotBlank() }
                    ?: senderUser?.username?.takeIf { it.isNotBlank() }
                    ?: "Contact-${senderId.take(6)}"

                val existing = localDataSource.getMessageById(msgId)

                when (type) {
                    "chat" -> {
                        if (existing == null) {
                            val text = json.getString("text")
                            val messageEntity = com.mesh.emergency.data.local.entity.MessageEntity(
                                entityId       = msgId,
                                messageId      = msgId,
                                conversationId = senderId,
                                senderId       = senderId,
                                senderName     = senderName,
                                recipientId    = receiverId,
                                content        = text,
                                timestamp      = timestamp,
                                createdAt      = timestamp,
                                updatedAt      = timestamp,
                                edited         = false,
                                deleted        = false,
                                deliveryStatus = com.mesh.emergency.data.local.entity.DbDeliveryStatus.DELIVERED,
                                readStatus     = "UNREAD",
                                syncState      = "SYNCED",
                                editHistory    = emptyList(),
                                type           = com.mesh.emergency.data.local.entity.DbMessageType.TEXT,
                                priority       = com.mesh.emergency.data.local.entity.DbMessagePriority.MEDIUM,
                                expiryTime     = timestamp + 86_400_000L,
                                retryCount     = 0
                            )
                            val conversationEntity = com.mesh.emergency.data.local.entity.ConversationEntity(
                                entityId      = senderId,
                                title         = senderName,
                                lastMessageId = msgId,
                                unreadCount   = 1,
                                updatedAt     = timestamp
                            )
                            localDataSource.insertMessage(messageEntity)
                            localDataSource.insertConversation(conversationEntity)

                            messageNotifier.notifyPrivateMessage(msgId, senderId, senderName, text)
                            triggerInboundNotification(msgId, senderName, text, isGlobal = false, sourceId = senderId)

                            // Acknowledge delivery immediately
                            sendDeliveryReceipt(msgId, localUserId, senderId)
                        }
                    }
                    "edit" -> {
                        val text = json.getString("text")
                        if (existing != null && timestamp > existing.updatedAt) {
                            val history = existing.editHistory.toMutableList()
                            if (!history.contains(existing.content)) {
                                history.add(existing.content)
                            }
                            val updated = existing.copy(
                                content = text,
                                edited = true,
                                updatedAt = timestamp,
                                editHistory = history
                            )
                            localDataSource.insertMessage(updated)
                        }
                    }
                    "delete" -> {
                        if (existing != null && timestamp > existing.updatedAt) {
                            val updated = existing.copy(
                                deleted = true,
                                updatedAt = timestamp
                            )
                            localDataSource.insertMessage(updated)
                        }
                    }
                    "delivery_receipt" -> {
                        if (existing != null && existing.deliveryStatus != com.mesh.emergency.data.local.entity.DbDeliveryStatus.DELIVERED) {
                            val updated = existing.copy(
                                deliveryStatus = com.mesh.emergency.data.local.entity.DbDeliveryStatus.DELIVERED
                            )
                            localDataSource.insertMessage(updated)
                        }
                    }
                    "read_receipt" -> {
                        if (existing != null) {
                            val updated = existing.copy(
                                deliveryStatus = com.mesh.emergency.data.local.entity.DbDeliveryStatus.DELIVERED,
                                readStatus = "READ"
                            )
                            localDataSource.insertMessage(updated)
                        }
                    }
                }
            } else {
                // Forward packet to peers (Mesh Routing)
                val actionKey = "msg:${msgId}:${type}:${timestamp}"
                if (seenPacketIds.add(actionKey)) {
                    Timber.d("MSG_FLOW: Message not for us. Forwarding to mesh — id=$msgId to=$receiverId")
                    reBroadcastPacket("MSG:$jsonStr")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "MSG_FLOW: Failed to parse inbound MSG: packet — $jsonStr")
        }
    }

    private fun reBroadcastPacket(payloadStr: String) {
        val payload = payloadStr.toByteArray(Charsets.UTF_8)
        sendViaConnectedClients(payload)
        sendViaGattClient(payload)
    }

    private fun triggerInboundNotification(
        messageId: String,
        senderName: String,
        text: String,
        isGlobal: Boolean,
        sourceId: String = "global"
    ) {
        scope.launch {
            if (com.mesh.emergency.app.MeshApplication.AppLifecycleTracker.isAppInForeground) {
                playReceiveSound()
            } else {
                val alert = com.mesh.emergency.core.notification.AlertModel(
                    id = messageId,
                    type = com.mesh.emergency.core.notification.AlertType.MESSAGE_ALERT,
                    title = senderName,
                    description = if (isGlobal) "[Global] $text" else text,
                    priority = com.mesh.emergency.core.notification.AlertPriority.NORMAL,
                    timestamp = System.currentTimeMillis(),
                    source = sourceId,
                    status = "ACTIVE"
                )
                notificationManager.showNotification(alert)
            }
        }
    }

    private fun playReceiveSound() {
        try {
            val uri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = android.media.RingtoneManager.getRingtone(context, uri)
            ringtone.play()
        } catch (e: Exception) {
            Timber.e(e, "Failed to play receive sound")
        }
    }

    fun sendDeliveryReceipt(messageId: String, senderId: String, recipientId: String) {
        val json = org.json.JSONObject().apply {
            put("type", "delivery_receipt")
            put("id", messageId)
            put("from", senderId)
            put("to", recipientId)
            put("ts", System.currentTimeMillis())
        }
        reBroadcastPacket("MSG:$json")
    }

    fun sendReadReceipt(messageId: String, senderId: String, recipientId: String) {
        val json = org.json.JSONObject().apply {
            put("type", "read_receipt")
            put("id", messageId)
            put("from", senderId)
            put("to", recipientId)
            put("ts", System.currentTimeMillis())
        }
        reBroadcastPacket("MSG:$json")
    }

    fun sendPrivateMessageEdit(messageId: String, senderId: String, recipientId: String, text: String) {
        val json = org.json.JSONObject().apply {
            put("type", "edit")
            put("id", messageId)
            put("from", senderId)
            put("to", recipientId)
            put("text", text)
            put("ts", System.currentTimeMillis())
        }
        reBroadcastPacket("MSG:$json")
    }

    fun sendPrivateMessageDelete(messageId: String, senderId: String, recipientId: String) {
        val json = org.json.JSONObject().apply {
            put("type", "delete")
            put("id", messageId)
            put("from", senderId)
            put("to", recipientId)
            put("ts", System.currentTimeMillis())
        }
        reBroadcastPacket("MSG:$json")
    }

    suspend fun onPeerConnected(remoteUserId: String) {
        Timber.d("SYNC_FLOW: Peer connected remoteUserId=$remoteUserId. Draining queue and syncing history...")
        
        // 1. Drain pending queue
        drainQueue()

        // 2. Synchronize recent global history (up to 50 items)
        val recentGlobal = localDataSource.getGlobalMessages().firstOrNull()?.take(50) ?: emptyList()
        for (msg in recentGlobal) {
            val json = org.json.JSONObject().apply {
                put("type", "chat")
                put("id", msg.messageId)
                put("from", msg.senderId)
                put("name", msg.senderName)
                put("text", msg.content)
                put("ts", msg.updatedAt)
            }.toString()
            val payload = "GCHAT:$json".toByteArray(Charsets.UTF_8)
            sendPayloadDirect(payload)
        }
    }

    suspend fun drainQueue() {
        try {
            // Drain private messages
            val pendingPrivate = localDataSource.getPendingMessages().firstOrNull() ?: emptyList()
            for (msg in pendingPrivate) {
                val json = org.json.JSONObject().apply {
                    put("type", "chat")
                    put("id", msg.entityId)
                    put("from", msg.senderId)
                    put("to", msg.recipientId)
                    put("text", msg.content)
                    put("ts", msg.timestamp)
                }.toString()
                val payload = "MSG:$json".toByteArray(Charsets.UTF_8)
                val success = sendPayloadDirect(payload)
                if (success) {
                    val updated = msg.copy(
                        deliveryStatus = com.mesh.emergency.data.local.entity.DbDeliveryStatus.SENT,
                        syncState = "SYNCED"
                    )
                    localDataSource.insertMessage(updated)
                }
            }

            // Drain global messages
            val allGlobal = localDataSource.getGlobalMessages().firstOrNull() ?: emptyList()
            val pendingGlobal = allGlobal.filter { it.deliveryStatus == "QUEUED" || it.deliveryStatus == "SENDING" }
            for (msg in pendingGlobal) {
                val sent = sendGlobalMessage(
                    messageId = msg.messageId,
                    senderId = msg.senderId,
                    senderName = msg.senderName,
                    text = msg.content
                )
                if (sent) {
                    localDataSource.updateGlobalMessageStatus(msg.messageId, "SENT")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "SYNC_FLOW: Failed to drain queue")
        }
    }

    private fun sendPayloadDirect(payload: ByteArray): Boolean {
        val serverSent = sendViaConnectedClients(payload)
        val clientSent = sendViaGattClient(payload)
        return serverSent || clientSent
    }

    // ── Identity helpers ─────────────────────────────────────────────────────

    /**
     * Reads the local user profile from Room and encodes it as an `IDENTITY:{json}` BLE packet.
     * Returns null if no profile has been set up yet (first-run before onboarding completes).
     */
    private suspend fun buildLocalIdentityPayload(): ByteArray? {
        return try {
            val localUser = localDataSource.getCurrentUser().firstOrNull()
            if (localUser == null) {
                Timber.w("PAIR_FLOW: buildLocalIdentityPayload — no local user profile found")
                return null
            }
            val json = org.json.JSONObject().apply {
                put("uid", localUser.entityId)
                put("un",  localUser.username.ifBlank { "Unknown" })
                put("pub", localUser.publicKey ?: "")
                put("ble", localBleAddress)
                put("dt",  "SMARTPHONE")
            }
            "IDENTITY:$json".toByteArray(Charsets.UTF_8)
        } catch (e: Exception) {
            Timber.e(e, "PAIR_FLOW: buildLocalIdentityPayload failed")
            null
        }
    }

    /**
     * Parses a peer's identity JSON (from either `IDENTITY:` or `REVERSE_HANDSHAKE` packets)
     * and upserts UserEntity + DeviceEntity into Room so the peer appears in Paired Devices.
     *
     * Expected JSON: `{"uid":"…","un":"…","pub":"…","ble":"…","dt":"…"}`
     */
    private suspend fun handlePeerIdentity(jsonStr: String, bleAddress: String?) {
        try {
            val json             = org.json.JSONObject(jsonStr)
            val remoteUserId     = json.getString("uid")
            val remoteUsername   = json.optString("un", "").ifBlank { "Contact-${remoteUserId.take(6)}" }
            val remotePublicKey  = json.optString("pub", "")
            val realBleAddress   = bleAddress?.takeIf { it != "02:00:00:00:00:00" }
                                    ?: json.optString("ble", "")
            val remoteDeviceType = json.optString("dt", "SMARTPHONE")

            Timber.d("PAIR_FLOW: handlePeerIdentity — uid=$remoteUserId un='$remoteUsername' ble=$realBleAddress")

            val userEntity = com.mesh.emergency.data.local.entity.UserEntity(
                entityId        = remoteUserId,
                username        = remoteUsername,
                profileImageRef = null,
                languagePreference = "en",
                createdTime     = System.currentTimeMillis(),
                updatedTime     = System.currentTimeMillis(),
                status          = "ACTIVE",
                isCurrentUser   = false,
                lastSeen        = System.currentTimeMillis(),
                trustedStatus   = true,
                nickname        = remoteUsername,
                publicKey       = remotePublicKey
            )
            val deviceEntity = com.mesh.emergency.data.local.entity.DeviceEntity(
                entityId      = remoteUserId,
                name          = remoteUsername,
                rssi          = -55,
                lastSeen      = System.currentTimeMillis(),
                deviceType    = remoteDeviceType,
                platformInfo  = "ANDROID",
                createdTime   = System.currentTimeMillis(),
                lastActiveTime = System.currentTimeMillis(),
                trustStatus   = com.mesh.emergency.data.local.entity.DbTrustStatus.TRUSTED,
                nickname      = remoteUsername,
                bleAddress    = realBleAddress
            )

            localDataSource.insertUser(userEntity)
            localDataSource.insertDevice(deviceEntity)
            Timber.d("DATABASE: Peer saved — id=$remoteUserId name='$remoteUsername' ble=$realBleAddress")
            Timber.d("PAIR_FLOW: Auto-pairing complete for $remoteUserId")
            
            // Immediately insert/update the node in network_nodes using real identity
            updateNodeInDb(realBleAddress, remoteUsername, -55, DbNodeStatus.ONLINE)

            // Trigger offline queue drain and history sync
            onPeerConnected(remoteUserId)
        } catch (e: Exception) {
            Timber.e(e, "PAIR_FLOW: handlePeerIdentity failed — $jsonStr")
        }
    }

    /**
     * Sends our local identity to the GATT server we are connected to as a client.
     * Called from [onDescriptorWrite] after TX notifications are successfully enabled.
     */
    private suspend fun sendIdentityAsClient() {
        val payload = buildLocalIdentityPayload() ?: run {
            Timber.w("PAIR_FLOW: sendIdentityAsClient — skipped (no local profile)")
            return
        }
        val sent = sendViaGattClient(payload)
        Timber.d("PAIR_FLOW: Identity written to server as GATT client — sent=$sent (${payload.size} bytes)")
    }

    private suspend fun updateNodeInDb(address: String, name: String, rssi: Int, status: DbNodeStatus) {
        if (address.isBlank() || address == "02:00:00:00:00:00") return
        try {
            // Check if there is a paired device associated with this BLE MAC
            val pairedDevices = try { localDataSource.getDevices().firstOrNull() } catch (e: Exception) { null }
            val pairedDevice = pairedDevices?.firstOrNull { 
                it.bleAddress.equals(address, ignoreCase = true) && 
                !it.entityId.contains(":") 
            }
            
            if (pairedDevice == null) {
                Timber.d("BLE_FLOW: Skipping updateNodeInDb for non-paired address=$address")
                return
            }

            val nodeId = pairedDevice.entityId // Use persistent remoteUserId
            val displayName = pairedDevice.nickname?.takeIf { it.isNotBlank() }
                ?: pairedDevice.name?.takeIf { it.isNotBlank() }
                ?: name

            val updated = NetworkNodeEntity(
                entityId = nodeId, // Store by remoteUserId to prevent duplicates from rotating MACs
                deviceId = displayName,
                nodeType = DbNodeType.PHONE_NODE,
                status = status,
                rssi = rssi,
                signalQuality = ((rssi + 100) * 1.43f).coerceIn(0f, 100f),
                lastSeen = System.currentTimeMillis(),
                connectionType = "BLE",
                batteryLevel = getBatteryLevel(context)
            )
            localDataSource.insertNode(updated)
            Timber.d("BLE_FLOW: Node updated in DB: nodeId=$nodeId name=$displayName rssi=$rssi status=$status")
        } catch (e: Exception) {
            Timber.e(e, "BLE_FLOW: Failed to update node in DB")
        }
    }

    fun getLocalBatteryLevel(): Int = getBatteryLevel(context)

    private fun getBatteryLevel(context: Context): Int {
        return try {
            val intent = context.registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
            val level = intent?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level >= 0 && scale > 0) (level * 100) / scale else -1
        } catch (e: Exception) {
            -1
        }
    }
}
