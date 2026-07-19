/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.test

import com.mesh.emergency.core.common.result.Result
import com.mesh.emergency.core.communication.CommunicationManager
import com.mesh.emergency.core.communication.CommunicationState
import com.mesh.emergency.core.communication.DeliveryResult
import com.mesh.emergency.core.communication.Transport
import com.mesh.emergency.core.communication.TransportType
import com.mesh.emergency.core.power.PowerEvent
import com.mesh.emergency.core.power.PowerManager
import com.mesh.emergency.core.power.PowerSavingMode
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf

/**
 * Pre-configured fake implementations of shared interfaces for use in unit tests.
 */
object MockFactory {

    // ── Stub LocalDataSource ──────────────────────────────────────────────────

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
        override fun getContacts(): Flow<List<UserEntity>> = flowOf(emptyList())
        override suspend fun getUserById(id: String): UserEntity? = user
        override suspend fun insertUser(user: UserEntity) = Unit
        override suspend fun deleteUser(user: UserEntity) = Unit

        override fun getDevices(): Flow<List<DeviceEntity>> = flowOf(devices)
        override suspend fun getDeviceById(id: String): DeviceEntity? = null
        override suspend fun clearAllDevices() = Unit
        override suspend fun insertDevice(device: DeviceEntity) = Unit
        override suspend fun deleteDevice(device: DeviceEntity) = Unit

        override fun getConversations(): Flow<List<ConversationEntity>> = flowOf(conversations)
        override suspend fun getConversationById(id: String): ConversationEntity? = null
        override suspend fun insertConversation(conversation: ConversationEntity) = Unit
        override suspend fun deleteConversation(conversation: ConversationEntity) = Unit

        override fun getMessagesForConversation(convId: String): Flow<List<MessageEntity>> = flowOf(messages)
        override suspend fun getMessageById(id: String): MessageEntity? = null
        override suspend fun insertMessage(message: MessageEntity) = Unit
        override suspend fun deleteMessage(message: MessageEntity) = Unit
        override suspend fun deleteAllMessages() = Unit

        override fun getNetworkNodes(): Flow<List<NetworkNodeEntity>> = flowOf(networkNodes)
        override suspend fun getNodeById(nodeId: String): NetworkNodeEntity? = null
        override suspend fun insertNode(node: NetworkNodeEntity) = Unit
        override suspend fun deleteNode(node: NetworkNodeEntity) = Unit

        override fun getLocationsForUser(userId: String): Flow<List<LocationEntity>> = flowOf(locations)
        override fun getAllLocations(): Flow<List<LocationEntity>> = flowOf(locations)
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

        override suspend fun clearDatabase() = Unit
    }

    // ── Stub CommunicationManager ─────────────────────────────────────────────

    fun stubCommunicationManager(
        sendResult: Result<DeliveryResult> = Result.Success(DeliveryResult.SENT)
    ): CommunicationManager = object : CommunicationManager {
        override val activeTransport: StateFlow<Transport?> = MutableStateFlow<Transport?>(null).asStateFlow()
        override val communicationState: StateFlow<CommunicationState> = MutableStateFlow(CommunicationState.DISCONNECTED).asStateFlow()
        override fun registerTransport(transport: Transport) = Unit
        override fun unregisterTransport(type: TransportType) = Unit
        override fun getTransports(): List<Transport> = emptyList()
        override fun getAvailableTransports(): List<TransportType> = emptyList()
        override suspend fun sendMessage(data: ByteArray): Result<DeliveryResult> = sendResult
        override fun receiveMessages(): Flow<ByteArray> = flowOf()
    }

    // ── Stub PowerManager ─────────────────────────────────────────────────────

    fun stubPowerManager(
        batteryLevel: Int = 85,
        isCharging: Boolean = false,
        isCritical: Boolean = false
    ): PowerManager = object : PowerManager {
        private val _currentMode = MutableStateFlow(
            when {
                isCritical || batteryLevel <= 10 -> PowerSavingMode.EMERGENCY
                batteryLevel <= 20 -> PowerSavingMode.SAVING
                else -> PowerSavingMode.NORMAL
            }
        )
        override val currentMode: StateFlow<PowerSavingMode> = _currentMode.asStateFlow()
        
        private val _powerEvents = MutableSharedFlow<PowerEvent>()
        override val powerEvents: SharedFlow<PowerEvent> = _powerEvents.asSharedFlow()

        override fun setPowerMode(mode: PowerSavingMode) {
            _currentMode.value = mode
        }
        override fun handleBatteryUpdate(level: Int, isCharging: Boolean) = Unit
    }

    // ── Stub NotificationServiceWrapper ───────────────────────────────────────

    fun stubNotificationServiceWrapper(
        notificationsEnabled: Boolean = true
    ): NotificationServiceWrapper = object : NotificationServiceWrapper {
        override fun areNotificationsEnabled(): Boolean = notificationsEnabled
        override fun cancelAllNotifications() = Unit
    }
}
