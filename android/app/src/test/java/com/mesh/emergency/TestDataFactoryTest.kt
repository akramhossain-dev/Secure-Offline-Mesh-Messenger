/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency

import com.mesh.emergency.test.TestDataFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying TestDataFactory generates valid entities with correct defaults.
 */
class TestDataFactoryTest {

    @Test
    fun fakeUser_hasExpectedDefaults() {
        val user = TestDataFactory.fakeUser()
        assertEquals("user-test-01", user.entityId)
        assertEquals("Test Operator", user.displayName)
        assertNotNull(user.publicKey)
    }

    @Test
    fun fakeDevice_hasExpectedDefaults() {
        val device = TestDataFactory.fakeDevice()
        assertEquals("AA:BB:CC:DD:EE:FF", device.macAddress)
        assertTrue(device.isTrusted)
        assertEquals("TRUSTED", device.trustState)
    }

    @Test
    fun fakeMessage_hasExpectedDefaults() {
        val msg = TestDataFactory.fakeMessage()
        assertEquals("msg-test-01", msg.entityId)
        assertEquals("TEXT", msg.type)
        assertEquals("PENDING", msg.status)
        assertTrue(msg.ttlMs > 0)
    }

    @Test
    fun fakeLocation_hasValidCoordinates() {
        val loc = TestDataFactory.fakeLocation()
        assertTrue(loc.latitude in -90.0..90.0)
        assertTrue(loc.longitude in -180.0..180.0)
        assertEquals("GPS", loc.provider)
    }

    @Test
    fun fakeNetworkNode_hasExpectedDefaults() {
        val node = TestDataFactory.fakeNetworkNode()
        assertEquals("MESH-NODE-001", node.nodeId)
        assertTrue(node.isActive)
        assertTrue(node.capabilities.isNotEmpty())
    }

    @Test
    fun fakeEmergencyEvent_hasHighPriority() {
        val event = TestDataFactory.fakeEmergencyEvent()
        assertEquals("SOS", event.type)
        assertEquals(100, event.priority)
        assertEquals("ACTIVE", event.status)
    }

    @Test
    fun fakeResource_hasExpectedDefaults() {
        val res = TestDataFactory.fakeResource()
        assertEquals("Water Supply", res.name)
        assertEquals("AVAILABLE", res.status)
        assertEquals(50, res.quantity)
    }

    @Test
    fun fakeVoiceMessage_hasValidOpusFormat() {
        val voice = TestDataFactory.fakeVoiceMessage()
        assertEquals("opus", voice.format)
        assertTrue(voice.duration > 0)
        assertTrue(voice.fileSize > 0)
    }

    @Test
    fun fakeLog_hasExpectedLevel() {
        val log = TestDataFactory.fakeLog()
        assertEquals("INFO", log.level)
        assertEquals("SYSTEM", log.category)
    }

    @Test
    fun fakeAlert_hasCriticalPriority() {
        val alert = TestDataFactory.fakeAlert()
        assertEquals("SOS Emergency", alert.title)
        assertEquals(com.mesh.emergency.core.notification.AlertPriority.CRITICAL, alert.priority)
    }

    @Test
    fun fakeMessage_allowsCustomFields() {
        val msg = TestDataFactory.fakeMessage(
            id = "custom-msg-id",
            content = "Custom emergency text",
            status = "DELIVERED"
        )
        assertEquals("custom-msg-id", msg.entityId)
        assertEquals("Custom emergency text", msg.content)
        assertEquals("DELIVERED", msg.status)
    }

    @Test
    fun randomId_isNotEmpty() {
        val id = TestDataFactory.randomId()
        assertTrue(id.isNotEmpty())
        assertTrue(id.length == 36) // UUID format
    }
}
