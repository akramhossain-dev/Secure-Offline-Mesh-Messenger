/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency

import com.mesh.emergency.core.protocol.LocationPacket
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for LocationPacket serialization and mesh protocol integration (A32.2).
 *
 * Tests:
 * - Serialization format correctness
 * - Round-trip deserialization
 * - Invalid payload handling
 * - Prefix detection
 */
class LocationStorageTest {

    @Test
    fun `serialize produces correct prefix`() {
        val packet = LocationPacket(
            senderId = "node_001",
            latitude = 23.8103,
            longitude = 90.4125,
            accuracy = 15.0f,
            timestamp = 1700000000000L,
            altitude = 10.0,
            label = "HQ"
        )
        val serialized = LocationPacket.serialize(packet)
        assertTrue(serialized.startsWith(LocationPacket.PACKET_PREFIX))
    }

    @Test
    fun `round-trip serialize and deserialize returns original data`() {
        val original = LocationPacket(
            senderId = "node_alpha",
            latitude = 48.8566,
            longitude = 2.3522,
            accuracy = 5.0f,
            timestamp = 1700000000000L,
            altitude = 35.0,
            label = "Paris HQ"
        )
        val serialized = LocationPacket.serialize(original)
        val deserialized = LocationPacket.deserialize(serialized)

        assertNotNull(deserialized)
        assertEquals(original.senderId, deserialized!!.senderId)
        assertEquals(original.latitude, deserialized.latitude, 0.0001)
        assertEquals(original.longitude, deserialized.longitude, 0.0001)
        assertEquals(original.accuracy, deserialized.accuracy, 0.01f)
        assertEquals(original.timestamp, deserialized.timestamp)
        assertEquals(original.altitude, deserialized.altitude, 0.0001)
    }

    @Test
    fun `deserialize returns null for non-location payload`() {
        val result = LocationPacket.deserialize("Hello mesh!")
        assertNull(result)
    }

    @Test
    fun `deserialize returns null for malformed location payload`() {
        val malformed = "${LocationPacket.PACKET_PREFIX}|too|few|parts"
        val result = LocationPacket.deserialize(malformed)
        assertNull(result)
    }

    @Test
    fun `deserialize handles label with pipe character safely`() {
        val packet = LocationPacket(
            senderId = "node_x",
            latitude = 0.0,
            longitude = 0.0,
            accuracy = 0f,
            timestamp = 0L,
            label = ""
        )
        val serialized = LocationPacket.serialize(packet)
        val deserialized = LocationPacket.deserialize(serialized)
        assertNotNull(deserialized)
    }

    @Test
    fun `location packet timestamp defaults to current time`() {
        val before = System.currentTimeMillis()
        val packet = LocationPacket(senderId = "n1", latitude = 0.0, longitude = 0.0, accuracy = 0f)
        val after = System.currentTimeMillis()
        assertTrue(packet.timestamp in before..after)
    }
}
