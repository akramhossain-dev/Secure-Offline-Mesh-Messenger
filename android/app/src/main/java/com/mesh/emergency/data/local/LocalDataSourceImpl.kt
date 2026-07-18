/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.local

import com.mesh.emergency.data.local.database.AppDatabase
import com.mesh.emergency.data.local.entity.ConversationEntity
import com.mesh.emergency.data.local.entity.DeliveryStatusEntity
import com.mesh.emergency.data.local.entity.DeviceEntity
import com.mesh.emergency.data.local.entity.EmergencyEventEntity
import com.mesh.emergency.data.local.entity.LocationEntity
import com.mesh.emergency.data.local.entity.MessageEntity
import com.mesh.emergency.data.local.entity.NetworkNodeEntity
import com.mesh.emergency.data.local.entity.ResourceEntity
import com.mesh.emergency.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local Data Source implementation forwarding transactional database actions to Room DAOs.
 */
@Singleton
class LocalDataSourceImpl @Inject constructor(
    private val database: AppDatabase
) : LocalDataSource {

    private val userDao = database.userDao()
    private val deviceDao = database.deviceDao()
    private val conversationDao = database.conversationDao()
    private val messageDao = database.messageDao()
    private val networkDao = database.networkDao()
    private val locationDao = database.locationDao()
    private val resourceDao = database.resourceDao()
    private val emergencyEventDao = database.emergencyEventDao()
    private val deliveryStatusDao = database.deliveryStatusDao()
    private val voiceMessageDao = database.voiceMessageDao()

    override fun getCurrentUser(): Flow<UserEntity?> = userDao.getCurrentUser()

    override fun getContacts(): Flow<List<UserEntity>> = userDao.getContacts()

    override suspend fun getUserById(id: String): UserEntity? = userDao.getUserById(id)

    override suspend fun insertUser(user: UserEntity) = userDao.insertUser(user)

    override suspend fun deleteUser(user: UserEntity) = userDao.deleteUser(user)

    override fun getDevices(): Flow<List<DeviceEntity>> = deviceDao.getDevices()

    override suspend fun getDeviceById(id: String): DeviceEntity? = deviceDao.getDeviceById(id)

    override suspend fun clearAllDevices() = deviceDao.clearAllDevices()

    override suspend fun insertDevice(device: DeviceEntity) = deviceDao.insertDevice(device)

    override suspend fun deleteDevice(device: DeviceEntity) = deviceDao.deleteDevice(device)

    override fun getConversations(): Flow<List<ConversationEntity>> =
        conversationDao.getConversations()

    override suspend fun getConversationById(id: String): ConversationEntity? =
        conversationDao.getConversationById(id)

    override suspend fun insertConversation(conversation: ConversationEntity) =
        conversationDao.insertConversation(conversation)

    override suspend fun deleteConversation(conversation: ConversationEntity) =
        conversationDao.deleteConversation(conversation)

    override fun getMessagesForConversation(convId: String): Flow<List<MessageEntity>> =
        messageDao.getMessagesForConversation(convId)

    override suspend fun getMessageById(id: String): MessageEntity? =
        messageDao.getMessageById(id)

    override suspend fun insertMessage(message: MessageEntity) =
        messageDao.insertMessage(message)

    override suspend fun deleteMessage(message: MessageEntity) =
        messageDao.deleteMessage(message)

    override fun getNetworkNodes(): Flow<List<NetworkNodeEntity>> = networkDao.getNetworkNodes()

    override suspend fun getNodeById(nodeId: String): NetworkNodeEntity? =
        networkDao.getNodeById(nodeId)

    override suspend fun insertNode(node: NetworkNodeEntity) = networkDao.insertNode(node)

    override suspend fun deleteNode(node: NetworkNodeEntity) = networkDao.deleteNode(node)

    override fun getLocationsForUser(userId: String): Flow<List<LocationEntity>> =
        locationDao.getLocationsForUser(userId)

    override suspend fun insertLocation(location: LocationEntity) =
        locationDao.insertLocation(location)

    override suspend fun deleteLocation(location: LocationEntity) =
        locationDao.deleteLocation(location)

    override fun getResources(): Flow<List<ResourceEntity>> = resourceDao.getResources()

    override suspend fun insertResource(resource: ResourceEntity) =
        resourceDao.insertResource(resource)

    override suspend fun deleteResource(resource: ResourceEntity) =
        resourceDao.deleteResource(resource)

    override fun getEmergencyEvents(): Flow<List<EmergencyEventEntity>> =
        emergencyEventDao.getEmergencyEvents()

    override suspend fun insertEmergencyEvent(event: EmergencyEventEntity) =
        emergencyEventDao.insertEmergencyEvent(event)

    override suspend fun deleteEmergencyEvent(event: EmergencyEventEntity) =
        emergencyEventDao.deleteEmergencyEvent(event)

    override fun getDeliveryStatusesForMessage(msgId: String): Flow<List<DeliveryStatusEntity>> =
        deliveryStatusDao.getDeliveryStatusesForMessage(msgId)

    override suspend fun insertDeliveryStatus(status: DeliveryStatusEntity) =
        deliveryStatusDao.insertDeliveryStatus(status)

    override suspend fun deleteDeliveryStatus(status: DeliveryStatusEntity) =
        deliveryStatusDao.deleteDeliveryStatus(status)

    override fun getVoiceMessages(): Flow<List<VoiceMessageEntity>> = voiceMessageDao.getVoiceMessages()

    override suspend fun insertVoiceMessage(voice: VoiceMessageEntity) = voiceMessageDao.insertVoiceMessage(voice)

    override suspend fun deleteVoiceMessage(id: String) = voiceMessageDao.deleteVoiceMessage(id)
}
