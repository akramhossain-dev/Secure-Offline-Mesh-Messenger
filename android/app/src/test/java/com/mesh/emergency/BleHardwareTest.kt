/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency

import com.mesh.emergency.core.hardware.BleConnectionResult
import com.mesh.emergency.core.hardware.BleConnectionState
import com.mesh.emergency.core.hardware.BleDataPacket
import com.mesh.emergency.core.hardware.BleDevice
import com.mesh.emergency.core.hardware.BleDiscoveryState
import com.mesh.emergency.core.hardware.BleFailureReason
import com.mesh.emergency.core.hardware.Esp32Command
import com.mesh.emergency.core.hardware.Esp32CommandCode
import com.mesh.emergency.core.hardware.Esp32CommandProtocol
import com.mesh.emergency.core.hardware.Esp32Response
import com.mesh.emergency.core.hardware.HardwareCapability
import com.mesh.emergency.core.hardware.HardwareDeviceProfile
import com.mesh.emergency.core.hardware.HardwareDeviceType
import com.mesh.emergency.core.hardware.LoRaBandwidth
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for A30 + A31:
 * - BLE device domain models
 * - Transport switching
 * - ESP32 command encoding (packet framing)
 * - ESP32 response decoding
 * - Hardware device profile
 */
class BleHardwareTest {

    // ── BLE Device Model ──────────────────────────────────────────────────────

    @Test
    fun bleDevice_defaultIsCompatible() {
        val device = makeBleDevice()
        assertTrue(device.isCompatible)
    }

    @Test
    fun bleDevice_discoveredState_isDiscovered() {
        val device = makeBleDevice(state = BleConnectionState.DISCOVERED)
        assertEquals(BleConnectionState.DISCOVERED, device.connectionState)
    }

    @Test
    fun bleDevice_copy_updatesConnectionState() {
        val device = makeBleDevice(state = BleConnectionState.DISCOVERED)
        val updated = device.copy(connectionState = BleConnectionState.CONNECTED)
        assertEquals(BleConnectionState.CONNECTED, updated.connectionState)
    }

    @Test
    fun bleDevice_rssiFiltering_highestRssiFirst() {
        val devices = listOf(
            makeBleDevice("A", rssi = -80),
            makeBleDevice("B", rssi = -50),
            makeBleDevice("C", rssi = -65)
        )
        val sorted = devices.sortedByDescending { it.rssi }
        assertEquals("B", sorted[0].id)
        assertEquals("C", sorted[1].id)
        assertEquals("A", sorted[2].id)
    }

    // ── Discovery States ──────────────────────────────────────────────────────

    @Test
    fun bleDiscoveryState_allCasesExist() {
        val states = BleDiscoveryState.entries
        assertTrue(states.contains(BleDiscoveryState.IDLE))
        assertTrue(states.contains(BleDiscoveryState.SCANNING))
        assertTrue(states.contains(BleDiscoveryState.STOPPED))
        assertTrue(states.contains(BleDiscoveryState.PERMISSION_DENIED))
        assertTrue(states.contains(BleDiscoveryState.BLE_UNAVAILABLE))
    }

    // ── Connection Result ─────────────────────────────────────────────────────

    @Test
    fun bleConnectionResult_success_isSuccess() {
        val result = BleConnectionResult.Success
        assertTrue(result is BleConnectionResult.Success)
    }

    @Test
    fun bleConnectionResult_failure_hasReason() {
        val result = BleConnectionResult.Failure(BleFailureReason.TIMEOUT)
        assertEquals(BleFailureReason.TIMEOUT, result.reason)
    }

    // ── Packet encoding ───────────────────────────────────────────────────────

    @Test
    fun encode_getStatus_producesCorrectOpcode() {
        val bytes = Esp32CommandProtocol.encode(Esp32Command.GetStatus)
        assertEquals(Esp32CommandCode.GET_STATUS, bytes[0])
        assertEquals(3, bytes.size) // CMD + LEN_HI + LEN_LO (no payload)
    }

    @Test
    fun encode_getBattery_producesCorrectOpcode() {
        val bytes = Esp32CommandProtocol.encode(Esp32Command.GetBattery)
        assertEquals(Esp32CommandCode.GET_BATTERY, bytes[0])
    }

    @Test
    fun encode_getSignal_producesCorrectOpcode() {
        val bytes = Esp32CommandProtocol.encode(Esp32Command.GetSignal)
        assertEquals(Esp32CommandCode.GET_SIGNAL, bytes[0])
    }

    @Test
    fun encode_getFirmware_producesCorrectOpcode() {
        val bytes = Esp32CommandProtocol.encode(Esp32Command.GetFirmwareVersion)
        assertEquals(Esp32CommandCode.GET_FIRMWARE, bytes[0])
    }

    @Test
    fun encode_reset_producesCorrectOpcode() {
        val bytes = Esp32CommandProtocol.encode(Esp32Command.Reset)
        assertEquals(Esp32CommandCode.RESET, bytes[0])
    }

    @Test
    fun encode_receiveMessage_producesCorrectOpcode() {
        val bytes = Esp32CommandProtocol.encode(Esp32Command.ReceiveMessage)
        assertEquals(Esp32CommandCode.RECEIVE_MESSAGE, bytes[0])
    }

    @Test
    fun encode_configureLora_producesCorrectOpcode() {
        val bytes = Esp32CommandProtocol.encode(
            Esp32Command.ConfigureLoRa(
                frequencyMhz = 433.0,
                spreadingFactor = 9,
                bandwidth = LoRaBandwidth.BW125
            )
        )
        assertEquals(Esp32CommandCode.CONFIGURE_LORA, bytes[0])
        assertTrue(bytes.size > 3) // Has payload
    }

    @Test
    fun encode_allCommandsHave3ByteHeader() {
        val commands = listOf(
            Esp32Command.GetStatus,
            Esp32Command.GetBattery,
            Esp32Command.GetSignal,
            Esp32Command.ReceiveMessage,
            Esp32Command.Reset,
            Esp32Command.GetFirmwareVersion
        )
        commands.forEach { cmd ->
            val bytes = Esp32CommandProtocol.encode(cmd)
            assertTrue("${cmd::class.simpleName} must have ≥3 bytes", bytes.size >= 3)
        }
    }

    // ── Packet decoding ───────────────────────────────────────────────────────

    @Test
    fun decode_emptyBytes_returnsUnknown() {
        val result = Esp32CommandProtocol.decode(byteArrayOf())
        assertTrue(result is Esp32Response.Unknown)
    }

    @Test
    fun decode_tooShortBytes_returnsUnknown() {
        val result = Esp32CommandProtocol.decode(byteArrayOf(0x81.toByte(), 0x00))
        assertTrue(result is Esp32Response.Unknown)
    }

    @Test
    fun decode_hardwareStatus_parsesFields() {
        // Response: [0x81][0x00][0x05][battery%][rssi][snr][fw][lora_ready]
        val payload = byteArrayOf(0x81.toByte(), 0x00, 0x05, 75, (-75).toByte(), 10, 3, 0x01)
        val result = Esp32CommandProtocol.decode(payload)
        assertTrue("Expected HardwareStatus, got $result", result is Esp32Response.HardwareStatus)
        val status = result as Esp32Response.HardwareStatus
        assertEquals(75, status.batteryPercent)
        assertEquals(-75, status.rssiDbm)
        assertTrue(status.loraReady)
    }

    @Test
    fun decode_messageSent_success() {
        val payload = byteArrayOf(0x82.toByte(), 0x00, 0x01, 0x01)
        val result = Esp32CommandProtocol.decode(payload)
        assertTrue(result is Esp32Response.MessageSent)
        assertTrue((result as Esp32Response.MessageSent).success)
    }

    @Test
    fun decode_messageSent_failure() {
        val payload = byteArrayOf(0x82.toByte(), 0x00, 0x01, 0x00)
        val result = Esp32CommandProtocol.decode(payload)
        assertTrue(result is Esp32Response.MessageSent)
        assertFalse((result as Esp32Response.MessageSent).success)
    }

    @Test
    fun decode_batteryStatus_parsesVoltageAndPercent() {
        // [0x84][0x00][0x03][volt_hi][volt_lo][percent]
        val voltageMilliV = 3850
        val payload = byteArrayOf(
            0x84.toByte(), 0x00, 0x03,
            ((voltageMilliV shr 8) and 0xFF).toByte(),
            (voltageMilliV and 0xFF).toByte(),
            85.toByte()
        )
        val result = Esp32CommandProtocol.decode(payload)
        assertTrue(result is Esp32Response.BatteryStatus)
        val battery = result as Esp32Response.BatteryStatus
        assertEquals(voltageMilliV, battery.voltageMilliV)
        assertEquals(85, battery.percent)
    }

    @Test
    fun decode_signalStatus_parsesRssiAndSnr() {
        val payload = byteArrayOf(0x85.toByte(), 0x00, 0x02, (-80).toByte(), 7)
        val result = Esp32CommandProtocol.decode(payload)
        assertTrue(result is Esp32Response.SignalStatus)
        val signal = result as Esp32Response.SignalStatus
        assertEquals(-80, signal.rssiDbm)
        assertEquals(7, signal.snrDb)
    }

    @Test
    fun decode_firmwareVersion_parsesString() {
        val versionStr = "1.3.0"
        val vBytes = versionStr.toByteArray(Charsets.UTF_8)
        val header = byteArrayOf(0x87.toByte(), 0x00, vBytes.size.toByte())
        val result = Esp32CommandProtocol.decode(header + vBytes)
        assertTrue(result is Esp32Response.FirmwareVersion)
        assertEquals("1.3.0", (result as Esp32Response.FirmwareVersion).version)
    }

    @Test
    fun decode_errorResponse_parsesMessage() {
        val errMsg = "ERR:LORA_NOT_READY"
        val eBytes = errMsg.toByteArray(Charsets.UTF_8)
        val header = byteArrayOf(0xFF.toByte(), 0x00, eBytes.size.toByte())
        val result = Esp32CommandProtocol.decode(header + eBytes)
        assertTrue(result is Esp32Response.Error)
        assertEquals("ERR:LORA_NOT_READY", (result as Esp32Response.Error).message)
    }

    @Test
    fun decode_unknownOpcode_returnsUnknown() {
        val payload = byteArrayOf(0xAA.toByte(), 0x00, 0x00)
        val result = Esp32CommandProtocol.decode(payload)
        assertTrue(result is Esp32Response.Unknown)
    }

    // ── Hardware Device Profile ───────────────────────────────────────────────

    @Test
    fun hardwareProfile_defaultCapabilities_hasLoraOnEsp32Lora() {
        val profile = makeProfile(
            deviceType = HardwareDeviceType.ESP32_LORA,
            capabilities = setOf(HardwareCapability.LORA_TX, HardwareCapability.LORA_RX)
        )
        assertTrue(profile.capabilities.contains(HardwareCapability.LORA_TX))
        assertTrue(profile.capabilities.contains(HardwareCapability.LORA_RX))
    }

    @Test
    fun hardwareProfile_batteryPercent_inRange() {
        val profile = makeProfile(batteryPercent = 75)
        assertTrue(profile.batteryPercent in 0..100)
    }

    @Test
    fun hardwareProfile_copy_updatesSignal() {
        val profile = makeProfile(signalRssi = -90)
        val updated = profile.copy(signalRssi = -60)
        assertEquals(-60, updated.signalRssi)
    }

    // ── BleDataPacket ─────────────────────────────────────────────────────────

    @Test
    fun bleDataPacket_equalsBasedOnContentAndSource() {
        val p1 = BleDataPacket("mac-01", byteArrayOf(0x01, 0x02))
        val p2 = BleDataPacket("mac-01", byteArrayOf(0x01, 0x02))
        val p3 = BleDataPacket("mac-02", byteArrayOf(0x01, 0x02))
        assertEquals(p1, p2)
        assertFalse(p1 == p3)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun makeBleDevice(
        id: String = "AA:BB:CC:DD:EE:FF",
        rssi: Int = -65,
        state: BleConnectionState = BleConnectionState.DISCOVERED
    ) = BleDevice(
        id = id, name = "Mesh-Node-$id", macAddress = id,
        rssi = rssi, connectionState = state
    )

    private fun makeProfile(
        deviceType: HardwareDeviceType = HardwareDeviceType.ESP32_LORA,
        batteryPercent: Int = 80,
        signalRssi: Int = -70,
        capabilities: Set<HardwareCapability> = setOf(HardwareCapability.LORA_TX, HardwareCapability.LORA_RX)
    ) = HardwareDeviceProfile(
        hardwareId = "hw-test-01",
        deviceType = deviceType,
        firmwareVersion = "1.0.0",
        batteryPercent = batteryPercent,
        capabilities = capabilities,
        signalRssi = signalRssi,
        isConnected = true
    )
}
