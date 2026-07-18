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
    val deviceType: String,
    val publicKeyRef: String,
    val timestamp: Long
)
