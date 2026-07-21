/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.hardware

import com.mesh.emergency.core.hardware.BleConnectionResult
import com.mesh.emergency.core.hardware.BleDevice
import com.mesh.emergency.core.hardware.BleDeviceRepository
import com.mesh.emergency.core.hardware.BleDiscoveryState
import com.mesh.emergency.core.hardware.Esp32Command
import com.mesh.emergency.core.hardware.Esp32CommandProtocol
import com.mesh.emergency.core.hardware.Esp32Response
import com.mesh.emergency.core.hardware.HardwareCapability
import com.mesh.emergency.core.hardware.HardwareCommandResult
import com.mesh.emergency.core.hardware.HardwareDeviceProfile
import com.mesh.emergency.core.hardware.HardwareDeviceType
import com.mesh.emergency.core.hardware.HardwareManager
import com.mesh.emergency.core.communication.Transport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A30.8 — HardwareManager implementation.
 *
 * Orchestrates BLE scanning via [BleDeviceRepository] and command exchange
 * via [Esp32CommandProtocol] and the active Bluetooth [Transport].
 *
 * A31.4 — Hardware Status Monitoring:
 * Automatically polls [Esp32Command.GetStatus] every 30 s when connected.
 */
@Singleton
class HardwareManagerImpl @Inject constructor(
    private val bleRepository: BleDeviceRepository,
    @javax.inject.Named("bluetooth") private val bluetoothTransport: Transport
) : HardwareManager {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override val knownDevices: StateFlow<List<BleDevice>> = bleRepository.discoveredDevices
    override val discoveryState: StateFlow<BleDiscoveryState> = bleRepository.discoveryState

    private val _connectedProfile = MutableStateFlow<HardwareDeviceProfile?>(null)
    override val connectedProfile: StateFlow<HardwareDeviceProfile?> = _connectedProfile.asStateFlow()

    private var connectedMac: String? = null

    init {
        // Bridge raw BLE data into typed ESP32 responses
        observeInboundData()
        // Start periodic status polling loop
        startStatusPollingLoop()
    }

    // ── A30.5 Discovery ───────────────────────────────────────────────────────

    override suspend fun startDiscovery(timeoutMs: Long) {
        bleRepository.startDiscovery()
    }

    override suspend fun stopDiscovery() {
        bleRepository.stopDiscovery()
    }

    // ── A30.6 Connection Management ───────────────────────────────────────────

    override suspend fun connectToDevice(macAddress: String): BleConnectionResult {
        val result = bleRepository.connectDevice(macAddress)
        if (result == BleConnectionResult.Success) {
            connectedMac = macAddress
            // Request hardware profile immediately after connection
            refreshHardwareStatus()
        }
        return result
    }

    override suspend fun disconnectCurrentDevice() {
        connectedMac?.let { mac ->
            bleRepository.disconnectDevice(mac)
            connectedMac = null
            _connectedProfile.value = null
        }
    }

    // ── A30.7 Command Send ────────────────────────────────────────────────────

    override suspend fun sendCommand(command: Esp32Command): HardwareCommandResult {
        if (connectedMac == null) return HardwareCommandResult.NoDeviceConnected

        val encoded = Esp32CommandProtocol.encode(command)
        val sendResult = bluetoothTransport.send(encoded)

        return when (sendResult) {
            is com.mesh.emergency.core.common.result.Result.Success ->
                HardwareCommandResult.Success(response = null)
            is com.mesh.emergency.core.common.result.Result.Error ->
                HardwareCommandResult.Failure(sendResult.exception.message ?: "Send failed")
            else ->
                HardwareCommandResult.Failure("Transport error")
        }
    }

    // ── A31.4 Hardware Status Monitoring ─────────────────────────────────────

    override suspend fun refreshHardwareStatus() {
        sendCommand(Esp32Command.GetStatus)
        sendCommand(Esp32Command.GetBattery)
        sendCommand(Esp32Command.GetSignal)
        sendCommand(Esp32Command.GetFirmwareVersion)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // A31.5 — Inbound Response Handler
    // ─────────────────────────────────────────────────────────────────────────

    private fun observeInboundData() {
        scope.launch {
            bleRepository.dataStream().collect { packet ->
                val response = Esp32CommandProtocol.decode(packet.payload)
                handleResponse(response, packet.sourceDeviceId)
            }
        }
    }

    private fun handleResponse(response: Esp32Response, sourceId: String) {
        when (response) {
            is Esp32Response.HardwareStatus -> {
                _connectedProfile.value = HardwareDeviceProfile(
                    hardwareId      = sourceId,
                    deviceType      = if (response.loraReady) HardwareDeviceType.ESP32_LORA else HardwareDeviceType.ESP32_ONLY,
                    firmwareVersion = "0.${response.firmwareVersionRaw}",
                    batteryPercent  = response.batteryPercent,
                    capabilities    = buildSet {
                        if (response.loraReady) addAll(listOf(HardwareCapability.LORA_TX, HardwareCapability.LORA_RX))
                        add(HardwareCapability.BATTERY_MONITOR)
                    },
                    signalRssi = response.rssiDbm,
                    isConnected = true
                )
            }

            is Esp32Response.BatteryStatus -> {
                _connectedProfile.value = _connectedProfile.value?.copy(
                    batteryPercent = response.percent,
                    lastPingMs = System.currentTimeMillis()
                )
            }

            is Esp32Response.SignalStatus -> {
                _connectedProfile.value = _connectedProfile.value?.copy(
                    signalRssi = response.rssiDbm,
                    lastPingMs = System.currentTimeMillis()
                )
            }

            is Esp32Response.FirmwareVersion -> {
                _connectedProfile.value = _connectedProfile.value?.copy(
                    firmwareVersion = response.version
                )
            }

            is Esp32Response.Error -> {
                // Log hardware errors — will integrate with LogManager (Phase A23)
            }

            else -> { /* Unhandled response type — future phases */ }
        }
    }

    // ── A31.6 Background Polling ──────────────────────────────────────────────

    private fun startStatusPollingLoop() {
        scope.launch {
            while (true) {
                delay(30_000L) // Poll every 30 seconds
                if (connectedMac != null) {
                    refreshHardwareStatus()
                }
            }
        }
    }

    fun destroy() {
        scope.cancel()
    }
}
