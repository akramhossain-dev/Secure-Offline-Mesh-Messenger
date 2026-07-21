/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.communication

import com.mesh.emergency.core.common.result.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Represents a physical or simulated network transceiver connection pathway.
 *
 * Every transport technology (BLE, LoRa, Wi-Fi Direct, NFC, USB Serial) must implement
 * this interface. The [CommunicationManager] and [MessagingService] interact exclusively
 * through this contract — never with concrete transport implementations.
 */
interface Transport {

    /** Transceiver type categorizer identifier. */
    val type: TransportType

    /** Current connection and capability status flow. */
    val status: StateFlow<TransportStatus>

    /**
     * Declared capabilities of this transport implementation.
     * Consumers can query this to decide whether to use optional features.
     */
    val capabilities: Set<TransportCapability>

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /**
     * Commands transport to start its hardware subsystem.
     * Called once at application startup to initialize hardware resources.
     * After [start], the transport may begin advertising and scanning.
     */
    suspend fun start(): Result<Unit>

    /**
     * Commands transport to stop its hardware subsystem and release all resources.
     * After [stop] the transport must not perform any further IO until [start] is called again.
     */
    suspend fun stop(): Result<Unit>

    /** Commands transport to bridge a peer connection. */
    suspend fun connect(): Result<Unit>

    /** Commands transport to drop the active peer connection. */
    suspend fun disconnect(): Result<Unit>

    // ── Discovery & Advertising ───────────────────────────────────────────────

    /**
     * Starts broadcasting this device's presence so peers can discover it.
     * No-op for transports that do not support discovery ([TransportCapability.DISCOVERABLE]).
     */
    suspend fun advertise(): Result<Unit>

    /**
     * Stops active advertising. Safe to call even if advertising was not started.
     */
    suspend fun stopAdvertising(): Result<Unit>

    /**
     * Starts active scanning / discovery for nearby peers.
     * No-op for transports that do not support discovery ([TransportCapability.DISCOVERABLE]).
     */
    suspend fun discover(): Result<Unit>

    /**
     * Stops active peer scanning / discovery.
     */
    suspend fun stopDiscovery(): Result<Unit>

    // ── Data Transfer ─────────────────────────────────────────────────────────

    /** Commands transport to enqueue/send raw bytes payload to connected peers. */
    suspend fun send(data: ByteArray): Result<Unit>

    /**
     * Sends a transport-level acknowledgment for a message.
     * No-op for transports that do not support [TransportCapability.ACKNOWLEDGEMENTS].
     */
    suspend fun sendAck(messageId: String): Result<Unit>

    /** Streams raw bytes payloads received by this transceiver. */
    fun receive(): Flow<ByteArray>

    // ── Diagnostics ───────────────────────────────────────────────────────────

    /**
     * Returns list of currently connected peer nodes visible to this transport.
     * Empty list if the transport is disconnected or doesn't support node discovery.
     */
    fun getConnectedNodes(): List<TransportNode>

    /**
     * Returns the current signal strength as an RSSI/SNR value in dBm.
     * Returns [Int.MIN_VALUE] if the transport is disconnected or not applicable.
     */
    fun getSignalStrength(): Int

    /**
     * Emits observable state changes for this transport, including diagnostics events.
     * Callers should prefer [status] for connection state; use this for richer events.
     */
    fun observeState(): Flow<TransportEvent>
}

// ── Transport Metadata Types ──────────────────────────────────────────────────

/**
 * Types identifying each transport technology.
 * Extending with new transports only requires adding a value here
 * and providing a [Transport] implementation — no other layer changes needed.
 */
enum class TransportType {
    BLUETOOTH,
    LORA,
    WIFI_DIRECT,
    NFC,
    USB_SERIAL,
    MOCK
}

/**
 * Optional capability flags that a [Transport] may declare.
 * The [CommunicationManager] and [MessagingService] use these to select
 * features without hard-coding transport-specific knowledge.
 */
enum class TransportCapability {
    /** Transport can broadcast to multiple peers simultaneously. */
    BROADCAST,
    /** Transport supports device discovery / advertising. */
    DISCOVERABLE,
    /** Transport supports transport-level message acknowledgements. */
    ACKNOWLEDGEMENTS,
    /** Transport supports signal strength measurement (RSSI / SNR). */
    SIGNAL_STRENGTH,
    /** Transport supports mesh routing (relay/forward packets). */
    MESH_ROUTING,
    /** Transport supports encryption at the transport layer. */
    TRANSPORT_ENCRYPTION
}

/**
 * Priority ordering for transport selection in [CommunicationManager].
 * Lower value = higher priority.
 * When multiple transports are available, the manager selects the one
 * with the lowest [ordinal].
 */
enum class TransportPriority(val rank: Int) {
    /** Bluetooth BLE — short range, high throughput, ubiquitous. */
    BLUETOOTH(1),
    /** LoRa Mesh — long range, low throughput, battery-efficient. */
    LORA(2),
    /** Wi-Fi Direct — medium range, highest throughput. */
    WIFI_DIRECT(3),
    /** NFC — very short range, tap-to-connect. */
    NFC(4),
    /** USB Serial — direct wired connection to external hardware. */
    USB_SERIAL(5),
    /** Mock / Loopback — testing only. */
    MOCK(99);

    companion object {
        /** Maps a [TransportType] to its selection priority rank. Lower = higher priority. */
        fun of(type: TransportType): Int = when (type) {
            TransportType.BLUETOOTH   -> BLUETOOTH.rank
            TransportType.LORA        -> LORA.rank
            TransportType.WIFI_DIRECT -> WIFI_DIRECT.rank
            TransportType.NFC         -> NFC.rank
            TransportType.USB_SERIAL  -> USB_SERIAL.rank
            TransportType.MOCK        -> MOCK.rank
        }
    }
}

/**
 * States representing hardware connectivity for a transport.
 */
enum class TransportStatus {
    CONNECTED,
    DISCONNECTED,
    CONNECTING,
    UNAVAILABLE
}

/**
 * Represents a peer node discovered or connected via a [Transport].
 */
data class TransportNode(
    /** Stable, transport-agnostic identifier for this peer. */
    val nodeId: String,
    /** Human-readable display name for this peer. */
    val displayName: String,
    /** Signal strength in dBm. [Int.MIN_VALUE] if unknown. */
    val rssi: Int = Int.MIN_VALUE,
    /** Timestamp of last contact in epoch milliseconds. */
    val lastSeenMs: Long = System.currentTimeMillis(),
    /** Which transport technology this node was seen on. */
    val transportType: TransportType
)

/**
 * Observable events emitted by a [Transport] for richer state observability.
 */
sealed interface TransportEvent {
    data class StatusChanged(val status: TransportStatus) : TransportEvent
    data class PeerConnected(val node: TransportNode)    : TransportEvent
    data class PeerDisconnected(val nodeId: String)      : TransportEvent
    data class PacketSent(val sizeBytes: Int)            : TransportEvent
    data class PacketReceived(val sizeBytes: Int)        : TransportEvent
    data class Error(val message: String, val cause: Throwable? = null) : TransportEvent
}
