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
import com.mesh.emergency.data.local.entity.DbTrustStatus
import com.mesh.emergency.data.local.entity.EmergencyEventEntity
import com.mesh.emergency.data.local.entity.DbEmergencyType
import com.mesh.emergency.data.local.entity.DbEmergencyStatus
import com.mesh.emergency.data.local.entity.LocationEntity
import com.mesh.emergency.data.local.entity.LogEntity
import com.mesh.emergency.data.local.entity.MessageEntity
import com.mesh.emergency.data.local.entity.DbDeliveryStatus
import com.mesh.emergency.data.local.entity.DbMessageType
import com.mesh.emergency.data.local.entity.DbMessagePriority
import com.mesh.emergency.data.local.entity.NetworkNodeEntity
import com.mesh.emergency.data.local.entity.DbNodeType
import com.mesh.emergency.data.local.entity.DbNodeStatus
import com.mesh.emergency.data.local.entity.ResourceEntity
import com.mesh.emergency.data.local.entity.DbResourceStatus
import com.mesh.emergency.data.local.entity.DbResourcePrivacy
import com.mesh.emergency.data.local.entity.UserEntity
import com.mesh.emergency.data.local.entity.VoiceMessageEntity
import java.util.UUID

/**
 * Centralized fake data factory for unit and integration tests.
 * All generated data is deterministic by default.
 */
object TestDataFactory {

    // ── Users ─────────────────────────────────────────────────────────────────

    fun fakeUser(
        id: String = "user-test-01",
        username: String = "Test Operator",
        nickname: String = "Operator"
    ) = UserEntity(
        entityId = id,
        username = username,
        profileImageRef = null,
        languagePreference = "en",
        createdTime = System.currentTimeMillis(),
        updatedTime = System.currentTimeMillis(),
        status = "ACTIVE",
        isCurrentUser = true,
        lastSeen = System.currentTimeMillis(),
        trustedStatus = true,
        nickname = nickname
    )

    // ── Devices ───────────────────────────────────────────────────────────────

    fun fakeDevice(
        id: String = "device-test-01",
        name: String = "Emergency Node Alpha",
        isTrusted: Boolean = true
    ) = DeviceEntity(
        entityId = id,
        name = name,
        rssi = -55,
        lastSeen = System.currentTimeMillis(),
        deviceType = "SMARTPHONE",
        platformInfo = "ANDROID",
        createdTime = System.currentTimeMillis(),
        lastActiveTime = System.currentTimeMillis(),
        trustStatus = if (isTrusted) DbTrustStatus.TRUSTED else DbTrustStatus.UNKNOWN,
        nickname = "Alpha"
    )

    // ── Messages ──────────────────────────────────────────────────────────────

    fun fakeMessage(
        id: String = "msg-test-01",
        senderId: String = "user-test-01",
        recipientId: String = "user-test-02",
        content: String = "Test message content",
        type: DbMessageType = DbMessageType.TEXT,
        status: DbDeliveryStatus = DbDeliveryStatus.PENDING,
        priority: DbMessagePriority = DbMessagePriority.MEDIUM
    ) = MessageEntity(
        entityId = id,
        conversationId = "conv-test-01",
        senderId = senderId,
        recipientId = recipientId,
        content = content,
        timestamp = System.currentTimeMillis(),
        deliveryStatus = status,
        type = type,
        priority = priority,
        expiryTime = System.currentTimeMillis() + 86_400_000L,
        retryCount = 0
    )

    // ── Conversations ─────────────────────────────────────────────────────────

    fun fakeConversation(
        id: String = "conv-test-01",
        title: String = "Test Conversation"
    ) = ConversationEntity(
        entityId = id,
        title = title,
        lastMessageId = "msg-test-01",
        unreadCount = 0,
        updatedAt = System.currentTimeMillis()
    )

    // ── Locations ─────────────────────────────────────────────────────────────

    fun fakeLocation(
        id: String = "loc-test-01",
        deviceId: String = "device-test-01",
        latitude: Double = 23.8103,   // Dhaka coordinates
        longitude: Double = 90.4125
    ) = LocationEntity(
        entityId = id,
        userId = "user-test-01",
        latitude = latitude,
        longitude = longitude,
        altitude = 10.0,
        accuracy = 5.0f,
        timestamp = System.currentTimeMillis(),
        provider = "GPS",
        deviceId = deviceId
    )

    // ── Network Nodes ─────────────────────────────────────────────────────────

    fun fakeNetworkNode(
        id: String = "node-test-01",
        deviceId: String = "device-test-01",
        rssi: Int = -65
    ) = NetworkNodeEntity(
        entityId = id,
        deviceId = deviceId,
        nodeType = DbNodeType.PHONE_NODE,
        status = DbNodeStatus.ONLINE,
        rssi = rssi,
        signalQuality = 80.0f,
        connectionType = "BLE",
        batteryLevel = 80,
        latitude = 23.8103,
        longitude = 90.4125,
        lastSeen = System.currentTimeMillis(),
        hopCount = 1,
        relayCapability = true,
        networkDistance = 1
    )

    // ── Emergency Events ──────────────────────────────────────────────────────

    fun fakeEmergencyEvent(
        id: String = "sos-test-01",
        senderId: String = "user-test-01"
    ) = EmergencyEventEntity(
        entityId = id,
        senderId = senderId,
        latitude = 23.8103,
        longitude = 90.4125,
        message = "Emergency SOS triggered",
        timestamp = System.currentTimeMillis(),
        isResolved = false,
        emergencyType = DbEmergencyType.SOS,
        priority = DbMessagePriority.CRITICAL,
        status = DbEmergencyStatus.CREATED,
        ttl = System.currentTimeMillis() + 3_600_000L
    )

    // ── Resources ─────────────────────────────────────────────────────────────

    fun fakeResource(
        id: String = "res-test-01",
        name: String = "Water Supply",
        quantity: Int = 50
    ) = ResourceEntity(
        entityId = id,
        ownerId = "user-test-01",
        name = name,
        type = "WATER",
        quantity = quantity,
        latitude = 23.8103,
        longitude = 90.4125,
        description = "50 liters of fresh water",
        availabilityStatus = DbResourceStatus.AVAILABLE,
        createdTime = System.currentTimeMillis(),
        updatedTime = System.currentTimeMillis(),
        privacyLevel = DbResourcePrivacy.PUBLIC,
        ttl = System.currentTimeMillis() + 86_400_000L
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
