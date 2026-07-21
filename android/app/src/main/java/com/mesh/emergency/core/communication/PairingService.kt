/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.communication

/**
 * Pairing service interface for BLE-specific device handshake operations.
 *
 * QR-based device pairing is a Bluetooth-specific protocol: the QR code encodes the BLE MAC address
 * and the reverse handshake is delivered over BLE GATT. This interface isolates those BLE-pairing
 * operations from the general [MessagingService] so that [QrPairViewModel] can depend on an
 * interface rather than [BluetoothTransportImpl] directly.
 *
 * If a future transport (e.g., NFC) replaces QR pairing, only the implementation changes.
 */
interface PairingService {

    /**
     * Returns the local BLE MAC address for embedding in the QR code handshake payload.
     * Returns an empty string if Bluetooth is unavailable.
     */
    val localBleAddress: String

    /**
     * Attempts to deliver [payload] to [targetBleAddress] via all available BLE paths:
     * 1. Notify connected GATT clients (server role)
     * 2. Write to connected GATT server (client role)
     * 3. Direct GATT connect to target MAC (if address is valid)
     * 4. Queue for delivery on next connection event
     *
     * @param payload The raw bytes of the reverse handshake packet.
     * @param targetBleAddress The BLE MAC address of the target device.
     */
    suspend fun queueAndDeliverReverseHandshake(payload: ByteArray, targetBleAddress: String)
}
