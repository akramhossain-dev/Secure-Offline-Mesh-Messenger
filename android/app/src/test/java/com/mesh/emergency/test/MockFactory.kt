/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.test

import com.mesh.emergency.core.common.result.Result
import com.mesh.emergency.core.communication.CommunicationManager
import com.mesh.emergency.core.communication.DeliveryResult
import com.mesh.emergency.core.communication.TransportType
import com.mesh.emergency.core.power.PowerManager
import com.mesh.emergency.core.system.NotificationServiceWrapper
import com.mesh.emergency.data.local.LocalDataSource
import com.mesh.emergency.data.local.entity.ConversationEntity
import com.mesh.emergency.data.local.entity.DeliveryStatusEntity
import com.mesh.emergency.data.local.entity.DeviceEntity
import com.mesh.emergency.data.local.entity.EmergencyEventEntity
import com.mesh.emergency.data.local.entity.LocationEntity
import com.mesh.emergency.data.local.entity.LogEntity
import com.mesh.emergency.data.local.entity.MessageEntity
import com.mesh.emergency.data.local.entity.NetworkNodeEntity
import com.mesh.emergency.data.local.entity.ResourceEntity
import com.mesh.emergency.data.local.entity.UserEntity
import com.mesh.emergency.data.local.entity.VoiceMessageEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Pre-configured fake implementations of shared interfaces for use in unit tests
 * without requiring Mockito. Each fake has sensible no-op or success defaults.
 */
object MockFactory {

    // ── Stub LocalDataSource ──────────────────────────────────────────────────

    /**
     * Returns a [LocalDataSource] stub where all queries return empty flows and
     * all writes succeed silently. Override individual methods as needed.
     */
    fun stubLocalDataSource(
        messages: List<MessageEntity> = emptyList(),
        logs: List<LogEntity> = emptyList(),
        emergencyEvents: List<EmergencyEventEntity> = emptyList(),
        locations: List<LocationEntity> = emptyList(),
        resources: List<ResourceEntity> = emptyList(),
        networkNodes: List<NetworkNodeEntity> = emptyList(),
        voiceMessages: List<VoiceMessageEntity> = emptyList(),
        devices: List<DeviceEntity> = emptyList(),
        conversations: List<ConversationEntity> = emptyList(),
        user: UserEntity? = null
    ): LocalDataSource = object : LocalDataSource {
        override fun getCurrentUser(): Flow<UserEntity?> = flowOf(user)
        override suspend fun insertUser(user: UserEntity) = Unit
        override suspend fun deleteUser(user: UserEntity) = Unit
        override fun getDevices(): Flow<List<DeviceEntity>> = flowOf(devices)
        override suspend fun insertDevice(device: DeviceEntity) = Unit
        override suspend fun deleteDevice(device: DeviceEntity) = Unit
        override suspend fun updateDeviceTrustStatus(deviceId: String, isTrusted: Boolean, trustState: String) = Unit
        override fun getMessages(): Flow<List<MessageEntity>> = flowOf(messages)
        override suspend fun insertMessage(message: MessageEntity) = Unit
        override suspend fun deleteMessage(message: MessageEntity) = Unit
        override suspend fun updateMessageStatus(messageId: String, status: String) = Unit
        override fun getConversations(): Flow<List<ConversationEntity>> = flowOf(conversations)
        override suspend fun insertConversation(conversation: ConversationEntity) = Unit
        override suspend fun deleteConversation(conversation: ConversationEntity) = Unit
        override fun getNetworkNodes(): Flow<List<NetworkNodeEntity>> = flowOf(networkNodes)
        override suspend fun insertNetworkNode(node: NetworkNodeEntity) = Unit
        override suspend fun deleteNetworkNode(node: NetworkNodeEntity) = Unit
        override fun getLocations(): Flow<List<LocationEntity>> = flowOf(locations)
        override suspend fun insertLocation(location: LocationEntity) = Unit
        override suspend fun deleteLocation(location: LocationEntity) = Unit
        override fun getResources(): Flow<List<ResourceEntity>> = flowOf(resources)
        override suspend fun insertResource(resource: ResourceEntity) = Unit
        override suspend fun deleteResource(resource: ResourceEntity) = Unit
        override fun getEmergencyEvents(): Flow<List<EmergencyEventEntity>> = flowOf(emergencyEvents)
        override suspend fun insertEmergencyEvent(event: EmergencyEventEntity) = Unit
        override suspend fun deleteEmergencyEvent(event: EmergencyEventEntity) = Unit
        override fun getDeliveryStatusesForMessage(msgId: String): Flow<List<DeliveryStatusEntity>> = flowOf(emptyList())
        override suspend fun insertDeliveryStatus(status: DeliveryStatusEntity) = Unit
        override suspend fun deleteDeliveryStatus(status: DeliveryStatusEntity) = Unit
        override fun getVoiceMessages(): Flow<List<VoiceMessageEntity>> = flowOf(voiceMessages)
        override suspend fun insertVoiceMessage(voice: VoiceMessageEntity) = Unit
        override suspend fun deleteVoiceMessage(id: String) = Unit
        override fun getLogs(): Flow<List<LogEntity>> = flowOf(logs)
        override suspend fun insertLog(log: LogEntity) = Unit
        override suspend fun clearLogs() = Unit
    }

    // ── Stub CommunicationManager ─────────────────────────────────────────────

    fun stubCommunicationManager(
        sendResult: Result<DeliveryResult> = Result.Success(DeliveryResult.SENT),
        currentTransport: TransportType = TransportType.BLUETOOTH
    ): CommunicationManager = object : CommunicationManager {
        override fun getActiveTransport(): TransportType = currentTransport
        override suspend fun sendMessage(payload: ByteArray): Result<DeliveryResult> = sendResult
        override suspend fun connect(): Result<Unit> = Result.Success(Unit)
        override suspend fun disconnect(): Result<Unit> = Result.Success(Unit)
    }

    // ── Stub PowerManager ─────────────────────────────────────────────────────

    fun stubPowerManager(
        batteryLevel: Int = 85,
        isCharging: Boolean = false,
        isCritical: Boolean = false
    ): PowerManager = object : PowerManager {
        override fun getBatteryLevel(): Int = batteryLevel
        override fun isCharging(): Boolean = isCharging
        override fun isBatteryCritical(): Boolean = isCritical
        override fun startMonitoring() = Unit
        override fun stopMonitoring() = Unit
    }

    // ── Stub NotificationServiceWrapper ───────────────────────────────────────

    fun stubNotificationServiceWrapper(
        notificationsEnabled: Boolean = true
    ): NotificationServiceWrapper = object : NotificationServiceWrapper {
        override fun areNotificationsEnabled(): Boolean = notificationsEnabled
        override fun cancelAllNotifications() = Unit
    }
}
