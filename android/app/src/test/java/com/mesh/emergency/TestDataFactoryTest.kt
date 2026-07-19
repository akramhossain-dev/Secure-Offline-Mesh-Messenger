/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency

import com.mesh.emergency.data.local.entity.DbDeliveryStatus
import com.mesh.emergency.data.local.entity.DbMessageType
import com.mesh.emergency.data.local.entity.DbMessagePriority
import com.mesh.emergency.data.local.entity.DbEmergencyType
import com.mesh.emergency.data.local.entity.DbEmergencyStatus
import com.mesh.emergency.data.local.entity.DbResourceStatus
import com.mesh.emergency.data.local.entity.DbNodeStatus
import com.mesh.emergency.data.local.entity.DbTrustStatus
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
        assertEquals("Test Operator", user.username)
        assertEquals("Operator", user.nickname)
    }

    @Test
    fun fakeDevice_hasExpectedDefaults() {
        val device = TestDataFactory.fakeDevice()
        assertEquals("device-test-01", device.entityId)
        assertEquals(DbTrustStatus.TRUSTED, device.trustStatus)
    }

    @Test
    fun fakeMessage_hasExpectedDefaults() {
        val msg = TestDataFactory.fakeMessage()
        assertEquals("msg-test-01", msg.entityId)
        assertEquals(DbMessageType.TEXT, msg.type)
        assertEquals(DbDeliveryStatus.PENDING, msg.deliveryStatus)
        assertTrue(msg.expiryTime > 0)
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
        assertEquals("device-test-01", node.deviceId)
        assertEquals(DbNodeStatus.ONLINE, node.status)
    }

    @Test
    fun fakeEmergencyEvent_hasHighPriority() {
        val event = TestDataFactory.fakeEmergencyEvent()
        assertEquals(DbEmergencyType.SOS, event.emergencyType)
        assertEquals(DbMessagePriority.CRITICAL, event.priority)
        assertEquals(DbEmergencyStatus.CREATED, event.status)
    }

    @Test
    fun fakeResource_hasExpectedDefaults() {
        val res = TestDataFactory.fakeResource()
        assertEquals("Water Supply", res.name)
        assertEquals(DbResourceStatus.AVAILABLE, res.availabilityStatus)
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
            status = DbDeliveryStatus.DELIVERED
        )
        assertEquals("custom-msg-id", msg.entityId)
        assertEquals("Custom emergency text", msg.content)
        assertEquals(DbDeliveryStatus.DELIVERED, msg.deliveryStatus)
    }

    @Test
    fun randomId_isNotEmpty() {
        val id = TestDataFactory.randomId()
        assertTrue(id.isNotEmpty())
        assertTrue(id.length == 36) // UUID format
    }
}
