/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.test

import com.mesh.emergency.core.notification.AlertModel
import com.mesh.emergency.core.notification.AlertPriority
import com.mesh.emergency.core.notification.AlertType
import com.mesh.emergency.data.local.entity.ConversationEntity
import com.mesh.emergency.data.local.entity.DeviceEntity
import com.mesh.emergency.data.local.entity.EmergencyEventEntity
import com.mesh.emergency.data.local.entity.LocationEntity
import com.mesh.emergency.data.local.entity.LogEntity
import com.mesh.emergency.data.local.entity.MessageEntity
import com.mesh.emergency.data.local.entity.NetworkNodeEntity
import com.mesh.emergency.data.local.entity.ResourceEntity
import com.mesh.emergency.data.local.entity.UserEntity
import com.mesh.emergency.data.local.entity.VoiceMessageEntity
import java.util.UUID

/**
 * Centralized fake data factory for unit and integration tests.
 *
 * All generated data is deterministic by default (fixed seeds / IDs), but
 * callers may override individual fields via named parameters.
 */
object TestDataFactory {

    // ── Users ─────────────────────────────────────────────────────────────────

    fun fakeUser(
        id: String = "user-test-01",
        displayName: String = "Test Operator",
        deviceId: String = "device-test-01",
        publicKey: String = "MFkwEwYHKoZIzj0CAQYFK4EEAAoDQgAE==",
        isOnline: Boolean = false
    ) = UserEntity(
        entityId = id,
        displayName = displayName,
        deviceId = deviceId,
        publicKey = publicKey,
        isOnline = isOnline
    )

    // ── Devices ───────────────────────────────────────────────────────────────

    fun fakeDevice(
        id: String = "device-test-01",
        name: String = "Emergency Node Alpha",
        macAddress: String = "AA:BB:CC:DD:EE:FF",
        isTrusted: Boolean = true
    ) = DeviceEntity(
        entityId = id,
        name = name,
        macAddress = macAddress,
        isTrusted = isTrusted,
        publicKey = "MFkwEwYHKoZIzj0CAQYFK4EEAAoDQgAE==",
        lastSeen = System.currentTimeMillis(),
        nickname = "Alpha",
        trustState = if (isTrusted) "TRUSTED" else "UNKNOWN"
    )

    // ── Messages ──────────────────────────────────────────────────────────────

    fun fakeMessage(
        id: String = "msg-test-01",
        senderId: String = "user-test-01",
        receiverId: String = "user-test-02",
        content: String = "Test message content",
        type: String = "TEXT",
        status: String = "PENDING",
        ttlMs: Long = 86_400_000L
    ) = MessageEntity(
        entityId = id,
        conversationId = "conv-test-01",
        senderId = senderId,
        receiverId = receiverId,
        content = content,
        type = type,
        status = status,
        timestamp = System.currentTimeMillis(),
        ttlMs = ttlMs,
        retryCount = 0,
        checksum = "abc123checksum"
    )

    // ── Conversations ─────────────────────────────────────────────────────────

    fun fakeConversation(
        id: String = "conv-test-01",
        participantId: String = "user-test-02",
        lastMessage: String = "Test message content"
    ) = ConversationEntity(
        entityId = id,
        participantId = participantId,
        lastMessage = lastMessage,
        lastMessageTime = System.currentTimeMillis(),
        unreadCount = 0
    )

    // ── Locations ─────────────────────────────────────────────────────────────

    fun fakeLocation(
        id: String = "loc-test-01",
        deviceId: String = "device-test-01",
        latitude: Double = 23.8103,   // Dhaka coordinates
        longitude: Double = 90.4125
    ) = LocationEntity(
        entityId = id,
        deviceId = deviceId,
        latitude = latitude,
        longitude = longitude,
        altitude = 10.0,
        accuracy = 5.0f,
        timestamp = System.currentTimeMillis(),
        provider = "GPS"
    )

    // ── Network Nodes ─────────────────────────────────────────────────────────

    fun fakeNetworkNode(
        id: String = "node-test-01",
        nodeId: String = "MESH-NODE-001",
        rssi: Int = -65,
        isActive: Boolean = true
    ) = NetworkNodeEntity(
        entityId = id,
        nodeId = nodeId,
        displayName = "Field Node 01",
        transportType = "BLUETOOTH",
        rssi = rssi,
        isActive = isActive,
        lastSeen = System.currentTimeMillis(),
        batteryLevel = 80,
        firmwareVersion = "1.0.0",
        capabilities = listOf("BLE", "LORA")
    )

    // ── Emergency Events ──────────────────────────────────────────────────────

    fun fakeEmergencyEvent(
        id: String = "sos-test-01",
        senderId: String = "user-test-01",
        type: String = "SOS",
        priority: Int = 100
    ) = EmergencyEventEntity(
        entityId = id,
        senderId = senderId,
        type = type,
        priority = priority,
        latitude = 23.8103,
        longitude = 90.4125,
        message = "Emergency SOS triggered",
        status = "ACTIVE",
        category = "RESCUE",
        ttlMs = 3_600_000L,
        timestamp = System.currentTimeMillis()
    )

    // ── Resources ─────────────────────────────────────────────────────────────

    fun fakeResource(
        id: String = "res-test-01",
        name: String = "Water Supply",
        category: String = "WATER",
        quantity: Int = 50
    ) = ResourceEntity(
        entityId = id,
        ownerId = "user-test-01",
        name = name,
        category = category,
        quantity = quantity,
        unit = "liters",
        status = "AVAILABLE",
        latitude = 23.8103,
        longitude = 90.4125,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
        isPrivate = false,
        ttlMs = 86_400_000L
    )

    // ── Voice Messages ────────────────────────────────────────────────────────

    fun fakeVoiceMessage(
        id: String = "voice-test-01",
        senderId: String = "user-test-01",
        receiverId: String = "user-test-02"
    ) = VoiceMessageEntity(
        entityId = id,
        senderId = senderId,
        receiverId = receiverId,
        fileReference = "/storage/emulated/0/audio/voice_test.opus",
        duration = 5000L,
        fileSize = 4096L,
        format = "opus",
        quality = "NORMAL",
        timestamp = System.currentTimeMillis(),
        status = "QUEUED"
    )

    // ── Log Entries ───────────────────────────────────────────────────────────

    fun fakeLog(
        id: String = "log-test-01",
        level: String = "INFO",
        category: String = "SYSTEM",
        message: String = "System started normally"
    ) = LogEntity(
        entityId = id,
        level = level,
        category = category,
        message = message,
        timestamp = System.currentTimeMillis(),
        deviceId = "device-test-01",
        moduleName = "TestModule"
    )

    // ── Alerts ────────────────────────────────────────────────────────────────

    fun fakeAlert(
        id: String = "alert-test-01",
        type: AlertType = AlertType.SOS_ALERT,
        priority: AlertPriority = AlertPriority.CRITICAL,
        title: String = "SOS Emergency",
        description: String = "Operator triggered distress signal"
    ) = AlertModel(
        id = id,
        type = type,
        title = title,
        description = description,
        priority = priority,
        timestamp = System.currentTimeMillis(),
        source = "TestEmergencyManager",
        status = "ACTIVE"
    )

    // ── Random ID helper ──────────────────────────────────────────────────────

    fun randomId(): String = UUID.randomUUID().toString()
}
