/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.local.database

import androidx.room.TypeConverter
import com.mesh.emergency.data.local.entity.DbDeliveryStatus
import com.mesh.emergency.data.local.entity.DbEmergencyStatus
import com.mesh.emergency.data.local.entity.DbEmergencyType
import com.mesh.emergency.data.local.entity.DbMessagePriority
import com.mesh.emergency.data.local.entity.DbMessageType
import com.mesh.emergency.data.local.entity.DbNodeStatus
import com.mesh.emergency.data.local.entity.DbNodeType
import com.mesh.emergency.data.local.entity.DbResourcePrivacy
import com.mesh.emergency.data.local.entity.DbResourceStatus
import com.mesh.emergency.data.local.entity.DbTrustStatus

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

    @TypeConverter
    fun fromTrustStatus(status: DbTrustStatus): String = status.name

    @TypeConverter
    fun toTrustStatus(value: String): DbTrustStatus =
        DbTrustStatus.valueOf(value)

    @TypeConverter
    fun fromEmergencyType(type: DbEmergencyType): String = type.name

    @TypeConverter
    fun toEmergencyType(value: String): DbEmergencyType =
        DbEmergencyType.valueOf(value)

    @TypeConverter
    fun fromEmergencyStatus(status: DbEmergencyStatus): String = status.name

    @TypeConverter
    fun toEmergencyStatus(value: String): DbEmergencyStatus =
        DbEmergencyStatus.valueOf(value)

    @TypeConverter
    fun fromResourceStatus(status: DbResourceStatus): String = status.name

    @TypeConverter
    fun toResourceStatus(value: String): DbResourceStatus =
        DbResourceStatus.valueOf(value)

    @TypeConverter
    fun fromResourcePrivacy(privacy: DbResourcePrivacy): String = privacy.name

    @TypeConverter
    fun toResourcePrivacy(value: String): DbResourcePrivacy =
        DbResourcePrivacy.valueOf(value)

    @TypeConverter
    fun fromNodeType(type: DbNodeType): String = type.name

    @TypeConverter
    fun toNodeType(value: String): DbNodeType =
        DbNodeType.valueOf(value)

    @TypeConverter
    fun fromNodeStatus(status: DbNodeStatus): String = status.name

    @TypeConverter
    fun toNodeStatus(value: String): DbNodeStatus =
        DbNodeStatus.valueOf(value)

    @TypeConverter
    fun fromStringList(list: List<String>?): String {
        if (list == null) return "[]"
        return org.json.JSONArray(list).toString()
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrEmpty()) return emptyList()
        val list = mutableListOf<String>()
        try {
            val jsonArray = org.json.JSONArray(value)
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getString(i))
            }
        } catch (e: Exception) {
            // Ignore
        }
        return list
    }
}
