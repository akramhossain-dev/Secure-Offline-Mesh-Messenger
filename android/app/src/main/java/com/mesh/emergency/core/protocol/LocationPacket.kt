/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.protocol

/**
 * Location packet for mesh-based coordinate sharing between nodes.
 *
 * Carried in the message protocol payload as a structured location broadcast.
 * All fields required for emergency location correlation.
 */
data class LocationPacket(
    /** Sender node identifier. */
    val senderId: String,
    /** WGS84 latitude in decimal degrees. */
    val latitude: Double,
    /** WGS84 longitude in decimal degrees. */
    val longitude: Double,
    /** Horizontal accuracy in meters. */
    val accuracy: Float,
    /** Unix epoch timestamp in milliseconds. */
    val timestamp: Long = System.currentTimeMillis(),
    /** Optional altitude in meters above sea level. */
    val altitude: Double = 0.0,
    /** Optional human-readable label for this location. */
    val label: String = ""
) {
    companion object {
        /** Protocol prefix used to identify location packets in raw payload. */
        const val PACKET_PREFIX = "[LOC]"

        /**
         * Serializes a [LocationPacket] to a compact pipe-delimited string.
         *
         * Format: `[LOC]|senderId|lat|lon|accuracy|timestamp|altitude|label`
         */
        fun serialize(packet: LocationPacket): String = buildString {
            append(PACKET_PREFIX)
            append("|${packet.senderId}")
            append("|${packet.latitude}")
            append("|${packet.longitude}")
            append("|${packet.accuracy}")
            append("|${packet.timestamp}")
            append("|${packet.altitude}")
            append("|${packet.label}")
        }

        /**
         * Deserializes a pipe-delimited string back into a [LocationPacket].
         * Returns null if the format is invalid.
         */
        fun deserialize(raw: String): LocationPacket? {
            return runCatching {
                if (!raw.startsWith(PACKET_PREFIX)) return null
                val parts = raw.removePrefix("$PACKET_PREFIX|").split("|")
                if (parts.size < 6) return null
                LocationPacket(
                    senderId = parts[0],
                    latitude = parts[1].toDouble(),
                    longitude = parts[2].toDouble(),
                    accuracy = parts[3].toFloat(),
                    timestamp = parts[4].toLong(),
                    altitude = if (parts.size > 5) parts[5].toDoubleOrNull() ?: 0.0 else 0.0,
                    label = if (parts.size > 6) parts[6] else ""
                )
            }.getOrNull()
        }
    }
}
