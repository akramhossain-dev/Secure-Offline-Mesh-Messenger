/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.protocol

/**
 * Universal packet format compatible across Android and external transceivers.
 */
data class Packet(
    val header: PacketHeader,
    val encryptedPayload: ByteArray,
    val authenticationTag: ByteArray? = null,
    val checksum: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Packet) return false
        if (header != other.header) return false
        if (!encryptedPayload.contentEquals(other.encryptedPayload)) return false
        if (authenticationTag != null) {
            if (other.authenticationTag == null) return false
            if (!authenticationTag.contentEquals(other.authenticationTag)) return false
        } else if (other.authenticationTag != null) return false
        if (checksum != other.checksum) return false
        return true
    }

    override fun hashCode(): Int {
        var result = header.hashCode()
        result = 31 * result + encryptedPayload.contentHashCode()
        result = 31 * result + (authenticationTag?.contentHashCode() ?: 0)
        result = 31 * result + checksum.hashCode()
        return result
    }
}
