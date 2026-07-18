/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.mesh.emergency.data.local.dao.ConversationDao
import com.mesh.emergency.data.local.dao.DeliveryStatusDao
import com.mesh.emergency.data.local.dao.DeviceDao
import com.mesh.emergency.data.local.dao.EmergencyEventDao
import com.mesh.emergency.data.local.dao.LocationDao
import com.mesh.emergency.data.local.dao.MessageDao
import com.mesh.emergency.data.local.dao.NetworkDao
import com.mesh.emergency.data.local.dao.ResourceDao
import com.mesh.emergency.data.local.dao.UserDao
import com.mesh.emergency.data.local.dao.VoiceMessageDao
import com.mesh.emergency.data.local.entity.ConversationEntity
import com.mesh.emergency.data.local.entity.DeliveryStatusEntity
import com.mesh.emergency.data.local.entity.DeviceEntity
import com.mesh.emergency.data.local.entity.EmergencyEventEntity
import com.mesh.emergency.data.local.entity.LocationEntity
import com.mesh.emergency.data.local.entity.MessageEntity
import com.mesh.emergency.data.local.entity.NetworkNodeEntity
import com.mesh.emergency.data.local.entity.ResourceEntity
import com.mesh.emergency.data.local.entity.UserEntity
import com.mesh.emergency.data.local.entity.VoiceMessageEntity

/**
 * Main persistent Room Database containing all offline tables.
 */
@Database(
    entities = [
        UserEntity::class,
        DeviceEntity::class,
        ConversationEntity::class,
        MessageEntity::class,
        NetworkNodeEntity::class,
        EmergencyEventEntity::class,
        LocationEntity::class,
        ResourceEntity::class,
        DeliveryStatusEntity::class,
        VoiceMessageEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun deviceDao(): DeviceDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun networkDao(): NetworkDao
    abstract fun locationDao(): LocationDao
    abstract fun resourceDao(): ResourceDao
    abstract fun emergencyEventDao(): EmergencyEventDao
    abstract fun deliveryStatusDao(): DeliveryStatusDao
    abstract fun voiceMessageDao(): VoiceMessageDao

    companion object {
        const val DATABASE_NAME = "mesh_emergency.db"
    }
}
