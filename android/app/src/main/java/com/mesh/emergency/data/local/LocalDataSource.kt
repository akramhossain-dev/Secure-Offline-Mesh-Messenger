/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.local

import com.mesh.emergency.data.local.entity.ConversationEntity
import com.mesh.emergency.data.local.entity.DeliveryStatusEntity
import com.mesh.emergency.data.local.entity.DeviceEntity
import com.mesh.emergency.data.local.entity.EmergencyEventEntity
import com.mesh.emergency.data.local.entity.LocationEntity
import com.mesh.emergency.data.local.entity.MessageEntity
import com.mesh.emergency.data.local.entity.GlobalMessageEntity
import com.mesh.emergency.data.local.entity.NetworkNodeEntity
import com.mesh.emergency.data.local.entity.ResourceEntity
import com.mesh.emergency.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

/**
 * Local Data Source abstraction separating Room database operations from repositories.
 */
interface LocalDataSource {

    // ── User Operations ───────────────────────────────────────────────────────
    fun getCurrentUser(): Flow<UserEntity?>
    fun getContacts(): Flow<List<UserEntity>>
    suspend fun getUserById(id: String): UserEntity?
    suspend fun insertUser(user: UserEntity)
    suspend fun deleteUser(user: UserEntity)

    // ── Device Operations ─────────────────────────────────────────────────────
    fun getDevices(): Flow<List<DeviceEntity>>
    suspend fun getDeviceById(id: String): DeviceEntity?
    suspend fun clearAllDevices()
    suspend fun insertDevice(device: DeviceEntity)
    suspend fun deleteDevice(device: DeviceEntity)

    // ── Conversation Operations ───────────────────────────────────────────────
    fun getConversations(): Flow<List<ConversationEntity>>
    suspend fun getConversationById(id: String): ConversationEntity?
    suspend fun insertConversation(conversation: ConversationEntity)
    suspend fun deleteConversation(conversation: ConversationEntity)

    // ── Message Operations ────────────────────────────────────────────────────
    fun getMessagesForConversation(convId: String): Flow<List<MessageEntity>>
    fun getPendingMessages(): Flow<List<MessageEntity>>
    suspend fun getMessageById(id: String): MessageEntity?
    suspend fun insertMessage(message: MessageEntity)
    suspend fun deleteMessage(message: MessageEntity)
    suspend fun deleteAllMessages()

    // ── Network Operations ────────────────────────────────────────────────────
    fun getNetworkNodes(): Flow<List<NetworkNodeEntity>>
    suspend fun getNodeById(nodeId: String): NetworkNodeEntity?
    suspend fun insertNode(node: NetworkNodeEntity)
    suspend fun deleteNode(node: NetworkNodeEntity)

    // ── Location Operations ───────────────────────────────────────────────────
    fun getLocationsForUser(userId: String): Flow<List<LocationEntity>>
    fun getAllLocations(): Flow<List<LocationEntity>>
    suspend fun insertLocation(location: LocationEntity)
    suspend fun deleteLocation(location: LocationEntity)

    // ── Resource Operations ───────────────────────────────────────────────────
    fun getResources(): Flow<List<ResourceEntity>>
    suspend fun insertResource(resource: ResourceEntity)
    suspend fun deleteResource(resource: ResourceEntity)

    // ── Emergency Event Operations ────────────────────────────────────────────
    fun getEmergencyEvents(): Flow<List<EmergencyEventEntity>>
    suspend fun insertEmergencyEvent(event: EmergencyEventEntity)
    suspend fun deleteEmergencyEvent(event: EmergencyEventEntity)

    // ── Delivery Status Operations ────────────────────────────────────────────
    fun getDeliveryStatusesForMessage(msgId: String): Flow<List<DeliveryStatusEntity>>
    suspend fun insertDeliveryStatus(status: DeliveryStatusEntity)
    suspend fun deleteDeliveryStatus(status: DeliveryStatusEntity)

    // ── Voice Message Operations ──────────────────────────────────────────────
    fun getVoiceMessages(): Flow<List<com.mesh.emergency.data.local.entity.VoiceMessageEntity>>
    suspend fun insertVoiceMessage(voice: com.mesh.emergency.data.local.entity.VoiceMessageEntity)
    suspend fun deleteVoiceMessage(id: String)

    // ── Log Operations ────────────────────────────────────────────────────────
    fun getLogs(): Flow<List<com.mesh.emergency.data.local.entity.LogEntity>>
    suspend fun insertLog(log: com.mesh.emergency.data.local.entity.LogEntity)
    suspend fun clearLogs()
    
    // ── Bulk Database Operations (A34.8) ──────────────────────────────────────
    suspend fun clearDatabase()

    // ── Global Chat Operations ────────────────────────────────────────────────
    fun getGlobalMessages(): kotlinx.coroutines.flow.Flow<List<GlobalMessageEntity>>
    suspend fun insertGlobalMessage(message: GlobalMessageEntity)
    suspend fun getGlobalMessageById(id: String): GlobalMessageEntity?
    suspend fun updateGlobalMessageStatus(id: String, status: String)
    suspend fun failStuckGlobalMessages()
    suspend fun deleteGlobalMessage(id: String)
}
