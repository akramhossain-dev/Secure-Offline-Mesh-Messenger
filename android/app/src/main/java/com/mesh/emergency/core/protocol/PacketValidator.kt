/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.protocol

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Auditor checking data frames validity, CRC checksums accuracy, and TTL lifetimes.
 */
@Singleton
class PacketValidator @Inject constructor() {

    /**
     * Inspects fields and verifies checksum status.
     */
    fun validate(packet: Packet): PacketValidationResult {
        val header = packet.header
        if (header.packetId.isBlank()) return PacketValidationResult.INVALID_HEADER
        if (header.senderId.isBlank()) return PacketValidationResult.INVALID_SENDER
        if (header.receiverId.isBlank()) return PacketValidationResult.INVALID_RECEIVER

        val now = System.currentTimeMillis()
        if (now > header.ttl) return PacketValidationResult.EXPIRED

        val expectedCrc = PacketSerializer.calculateChecksum(header, packet.encryptedPayload)
        if (expectedCrc != packet.checksum) return PacketValidationResult.CHECKSUM_MISMATCH

        return PacketValidationResult.VALID
    }
}

/**
 * Result outcomes for validation sweeps.
 */
enum class PacketValidationResult {
    VALID,
    INVALID_HEADER,
    INVALID_SENDER,
    INVALID_RECEIVER,
    EXPIRED,
    CHECKSUM_MISMATCH
}
