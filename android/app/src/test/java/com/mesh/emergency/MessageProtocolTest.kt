/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency

import com.mesh.emergency.core.protocol.DuplicateDetector
import com.mesh.emergency.core.protocol.Packet
import com.mesh.emergency.core.protocol.PacketHeader
import com.mesh.emergency.core.protocol.PacketSerializer
import com.mesh.emergency.core.protocol.PacketValidator
import com.mesh.emergency.core.protocol.PacketValidationResult
import com.mesh.emergency.data.local.entity.DbMessagePriority
import com.mesh.emergency.data.local.entity.DbMessageType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit test validating serialization roundtrip and duplicate packets detection.
 */
class MessageProtocolTest {

    private lateinit var duplicateDetector: DuplicateDetector
    private lateinit var packetValidator: PacketValidator

    @Before
    fun setUp() {
        duplicateDetector = DuplicateDetector()
        packetValidator = PacketValidator()
    }

    @Test
    fun testSerializationRoundtrip() {
        val payload = "payload data text message".toByteArray()
        val header = PacketHeader(
            packetId = "packet_id_1",
            senderId = "sender_usr",
            receiverId = "recipient_usr",
            messageType = DbMessageType.TEXT,
            priority = DbMessagePriority.MEDIUM,
            ttl = System.currentTimeMillis() + 60000L,
            timestamp = System.currentTimeMillis()
        )
        val checksum = PacketSerializer.calculateChecksum(header, payload)
        val originalPacket = Packet(header, payload, null, checksum)

        val json = PacketSerializer.serializeToJson(originalPacket)
        val deserialized = PacketSerializer.deserializeFromJson(json)

        assertEquals(originalPacket.header.packetId, deserialized.header.packetId)
        assertEquals(originalPacket.checksum, deserialized.checksum)
        assertTrue(originalPacket.encryptedPayload.contentEquals(deserialized.encryptedPayload))
    }

    @Test
    fun testPacketValidation_returnsValidForGoodPacket() {
        val payload = "hello validation".toByteArray()
        val header = PacketHeader(
            packetId = "packet_id_2",
            senderId = "sender",
            receiverId = "receiver",
            messageType = DbMessageType.TEXT,
            priority = DbMessagePriority.HIGH,
            ttl = System.currentTimeMillis() + 30000L,
            timestamp = System.currentTimeMillis()
        )
        val checksum = PacketSerializer.calculateChecksum(header, payload)
        val packet = Packet(header, payload, null, checksum)

        assertEquals(PacketValidationResult.VALID, packetValidator.validate(packet))
    }

    @Test
    fun testPacketValidation_detectsChecksumMismatch() {
        val payload = "good payload".toByteArray()
        val header = PacketHeader(
            packetId = "packet_id_3",
            senderId = "sender",
            receiverId = "receiver",
            messageType = DbMessageType.TEXT,
            priority = DbMessagePriority.HIGH,
            ttl = System.currentTimeMillis() + 30000L,
            timestamp = System.currentTimeMillis()
        )
        val packet = Packet(header, payload, null, 12345678L) // Bad Checksum

        assertEquals(PacketValidationResult.CHECKSUM_MISMATCH, packetValidator.validate(packet))
    }

    @Test
    fun testDuplicateDetector_prunesMatches() {
        assertFalse(duplicateDetector.isDuplicate("packet_id_x"))
        assertTrue(duplicateDetector.isDuplicate("packet_id_x")) // Duplicate
        assertFalse(duplicateDetector.isDuplicate("packet_id_y"))
    }
}
