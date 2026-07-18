/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.hardware

// ─────────────────────────────────────────────────────────────────────────────
// A31.2 — ESP32 Command Protocol
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Type-safe representation of every command the Android app can send to the
 * ESP32 hardware node over BLE GATT.
 *
 * Wire format (binary, little-endian):
 * ```
 * ┌──────────┬──────────┬────────────────────────────┐
 * │  CMD (1B)│  LEN (2B)│  PAYLOAD (variable)        │
 * └──────────┴──────────┴────────────────────────────┘
 * ```
 * All commands are defined in [Esp32CommandCode].
 */
sealed class Esp32Command {

    /** Request current hardware status (battery, signal, firmware). */
    data object GetStatus : Esp32Command()

    /**
     * Send a UTF-8 encoded message payload via LoRa.
     * @param recipientId Destination node identifier (max 20 chars).
     * @param payload     Message body bytes (max 240 bytes after framing).
     */
    data class SendMessage(
        val recipientId: String,
        val payload: ByteArray
    ) : Esp32Command() {
        override fun equals(other: Any?) = other is SendMessage &&
            recipientId == other.recipientId && payload.contentEquals(other.payload)
        override fun hashCode() = 31 * recipientId.hashCode() + payload.contentHashCode()
    }

    /** Retrieve a pending inbound message from the ESP32 receive buffer. */
    data object ReceiveMessage : Esp32Command()

    /** Request current battery voltage and percentage reading. */
    data object GetBattery : Esp32Command()

    /** Request current LoRa RSSI and SNR readings. */
    data object GetSignal : Esp32Command()

    /** Reset the ESP32 module. */
    data object Reset : Esp32Command()

    /** Request firmware version string. */
    data object GetFirmwareVersion : Esp32Command()

    /**
     * Configure LoRa radio parameters.
     * @param frequencyMhz Carrier frequency (e.g. 433.0, 868.0, 915.0)
     * @param spreadingFactor SF7–SF12
     * @param bandwidth      BW125, BW250, or BW500 kHz
     */
    data class ConfigureLoRa(
        val frequencyMhz: Double,
        val spreadingFactor: Int,
        val bandwidth: LoRaBandwidth
    ) : Esp32Command()
}

enum class LoRaBandwidth(val khz: Int) {
    BW125(125), BW250(250), BW500(500)
}

/**
 * Numeric opcodes for the binary wire protocol.
 * Must match the ESP32 firmware command handler table.
 */
object Esp32CommandCode {
    const val GET_STATUS        : Byte = 0x01
    const val SEND_MESSAGE      : Byte = 0x02
    const val RECEIVE_MESSAGE   : Byte = 0x03
    const val GET_BATTERY       : Byte = 0x04
    const val GET_SIGNAL        : Byte = 0x05
    const val RESET             : Byte = 0x06
    const val GET_FIRMWARE      : Byte = 0x07
    const val CONFIGURE_LORA    : Byte = 0x08
}

// ─────────────────────────────────────────────────────────────────────────────
// A31.3 — Packet Bridge Encoder / Decoder
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Serialises and deserialises [Esp32Command] instances to/from raw [ByteArray]
 * for transmission over the BLE GATT characteristic.
 *
 * This bridges the gap between typed domain commands and the low-level wire protocol.
 */
object Esp32CommandProtocol {

    private const val MAX_PAYLOAD_BYTES = 240

    // ── Encoding ─────────────────────────────────────────────────────────────

    /**
     * Encode an [Esp32Command] to the BLE wire format:
     * `[CMD:1][LEN_HI:1][LEN_LO:1][PAYLOAD:variable]`
     */
    fun encode(command: Esp32Command): ByteArray {
        val (opcode, payload) = when (command) {
            is Esp32Command.GetStatus         -> Esp32CommandCode.GET_STATUS       to byteArrayOf()
            is Esp32Command.ReceiveMessage    -> Esp32CommandCode.RECEIVE_MESSAGE  to byteArrayOf()
            is Esp32Command.GetBattery        -> Esp32CommandCode.GET_BATTERY      to byteArrayOf()
            is Esp32Command.GetSignal         -> Esp32CommandCode.GET_SIGNAL       to byteArrayOf()
            is Esp32Command.Reset             -> Esp32CommandCode.RESET            to byteArrayOf()
            is Esp32Command.GetFirmwareVersion -> Esp32CommandCode.GET_FIRMWARE   to byteArrayOf()
            is Esp32Command.SendMessage -> {
                val idBytes  = command.recipientId.take(20).toByteArray(Charsets.UTF_8)
                val body     = command.payload.take(MAX_PAYLOAD_BYTES).toByteArray()
                // Frame: [ID_LEN:1][ID:n][BODY:m]
                byteArrayOf(idBytes.size.toByte()) + idBytes + body
                Esp32CommandCode.SEND_MESSAGE to (byteArrayOf(idBytes.size.toByte()) + idBytes + body)
            }
            is Esp32Command.ConfigureLoRa -> {
                val freqInt = (command.frequencyMhz * 1_000).toInt() // Store as kHz integer
                Esp32CommandCode.CONFIGURE_LORA to byteArrayOf(
                    (freqInt shr 24).toByte(),
                    (freqInt shr 16).toByte(),
                    (freqInt shr 8).toByte(),
                    freqInt.toByte(),
                    command.spreadingFactor.toByte(),
                    command.bandwidth.khz.toByte()
                )
            }
        }

        val lenHi = ((payload.size shr 8) and 0xFF).toByte()
        val lenLo = (payload.size and 0xFF).toByte()
        return byteArrayOf(opcode, lenHi, lenLo) + payload
    }

    // ── Decoding ─────────────────────────────────────────────────────────────

    /**
     * Decode a raw response [ByteArray] from the ESP32 into an [Esp32Response].
     * Returns [Esp32Response.Unknown] if the bytes are malformed.
     */
    fun decode(bytes: ByteArray): Esp32Response {
        if (bytes.size < 3) return Esp32Response.Unknown(bytes)
        val responseCode = bytes[0]
        val payloadLen   = ((bytes[1].toInt() and 0xFF) shl 8) or (bytes[2].toInt() and 0xFF)
        val payload      = if (bytes.size >= 3 + payloadLen) bytes.sliceArray(3 until 3 + payloadLen)
                           else return Esp32Response.Malformed(bytes)

        return when (responseCode) {
            0x81.toByte() -> parseStatusResponse(payload)
            0x82.toByte() -> Esp32Response.MessageSent(success = payload.firstOrNull() == 0x01.toByte())
            0x83.toByte() -> Esp32Response.InboundMessage(
                senderId = String(payload.sliceArray(1 until (1 + (payload[0].toInt() and 0xFF)).coerceAtMost(payload.size)), Charsets.UTF_8),
                payload  = payload.sliceArray((1 + (payload[0].toInt() and 0xFF)).coerceAtMost(payload.size) until payload.size)
            )
            0x84.toByte() -> Esp32Response.BatteryStatus(
                voltageMilliV = ((payload[0].toInt() and 0xFF) shl 8) or (payload[1].toInt() and 0xFF),
                percent       = payload.getOrElse(2) { 0 }.toInt() and 0xFF
            )
            0x85.toByte() -> Esp32Response.SignalStatus(
                rssiDbm = payload.getOrElse(0) { -120 }.toInt(),
                snrDb   = payload.getOrElse(1) { 0 }.toInt()
            )
            0x87.toByte() -> Esp32Response.FirmwareVersion(
                version = String(payload, Charsets.UTF_8).trim()
            )
            0xFF.toByte() -> Esp32Response.Error(
                message = String(payload, Charsets.UTF_8).trim()
            )
            else -> Esp32Response.Unknown(bytes)
        }
    }

    private fun parseStatusResponse(payload: ByteArray): Esp32Response {
        if (payload.size < 4) return Esp32Response.Malformed(payload)
        return Esp32Response.HardwareStatus(
            batteryPercent    = payload[0].toInt() and 0xFF,
            rssiDbm           = payload[1].toInt(),
            snrDb             = payload[2].toInt(),
            firmwareVersionRaw = payload[3].toInt() and 0xFF,
            loraReady         = payload.getOrElse(4) { 0 } == 0x01.toByte()
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// A31.3 — ESP32 Response models
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Strongly-typed responses parsed from BLE characteristic notification bytes.
 */
sealed class Esp32Response {
    data class HardwareStatus(
        val batteryPercent: Int,
        val rssiDbm: Int,
        val snrDb: Int,
        val firmwareVersionRaw: Int,
        val loraReady: Boolean
    ) : Esp32Response()

    data class MessageSent(val success: Boolean)         : Esp32Response()
    data class InboundMessage(val senderId: String, val payload: ByteArray) : Esp32Response() {
        override fun equals(other: Any?) = other is InboundMessage &&
            senderId == other.senderId && payload.contentEquals(other.payload)
        override fun hashCode() = 31 * senderId.hashCode() + payload.contentHashCode()
    }
    data class BatteryStatus(val voltageMilliV: Int, val percent: Int) : Esp32Response()
    data class SignalStatus(val rssiDbm: Int, val snrDb: Int)          : Esp32Response()
    data class FirmwareVersion(val version: String)                     : Esp32Response()
    data class Error(val message: String)                               : Esp32Response()
    data class Unknown(val raw: ByteArray)                             : Esp32Response()
    data class Malformed(val raw: ByteArray)                           : Esp32Response()
}
