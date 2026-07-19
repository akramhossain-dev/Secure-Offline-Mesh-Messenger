/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.discovery.qr

/**
 * Metadata data schema representing handshake payload encoded inside pairing QR codes.
 */
data class QRHandshakeData(
    val version: Int = 1,
    val deviceId: String,
    val userId: String,
    val username: String = "",   // Human-readable display name for the contact
    val deviceType: String,
    val publicKeyRef: String,
    val timestamp: Long,
    val bleAddress: String = ""  // Bluetooth MAC address for direct GATT connection
)
