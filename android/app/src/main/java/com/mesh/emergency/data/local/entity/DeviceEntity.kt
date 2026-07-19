/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mesh.emergency.core.model.BaseEntity

/**
 * Database Entity mapping discovered or paired devices metadata logs.
 */
@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey override val entityId: String,
    val name: String,
    val rssi: Int,
    val lastSeen: Long,
    val deviceType: String = "SMARTPHONE",
    val platformInfo: String = "ANDROID",
    val createdTime: Long = System.currentTimeMillis(),
    val lastActiveTime: Long = System.currentTimeMillis(),
    val trustStatus: DbTrustStatus = DbTrustStatus.UNKNOWN,
    val nickname: String? = null,
    val bleAddress: String = ""  // Bluetooth MAC address for direct GATT connection
) : BaseEntity

/**
 * Peer trust level states.
 */
enum class DbTrustStatus {
    TRUSTED,
    PENDING,
    BLOCKED,
    UNKNOWN
}
