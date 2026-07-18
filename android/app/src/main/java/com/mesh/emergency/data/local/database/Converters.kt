/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.local.database

import androidx.room.TypeConverter
import com.mesh.emergency.data.local.entity.DbDeliveryStatus
import com.mesh.emergency.data.local.entity.DbMessagePriority
import com.mesh.emergency.data.local.entity.DbMessageType

/**
 * Type converters converting custom database enums to primitive strings for Room compilation.
 */
class Converters {

    @TypeConverter
    fun fromDeliveryStatus(status: DbDeliveryStatus): String = status.name

    @TypeConverter
    fun toDeliveryStatus(value: String): DbDeliveryStatus =
        DbDeliveryStatus.valueOf(value)

    @TypeConverter
    fun fromMessageType(type: DbMessageType): String = type.name

    @TypeConverter
    fun toMessageType(value: String): DbMessageType =
        DbMessageType.valueOf(value)

    @TypeConverter
    fun fromMessagePriority(priority: DbMessagePriority): String = priority.name

    @TypeConverter
    fun toMessagePriority(value: String): DbMessagePriority =
        DbMessagePriority.valueOf(value)
}
