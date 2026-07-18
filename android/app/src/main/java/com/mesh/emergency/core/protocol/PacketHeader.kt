/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.protocol

import com.mesh.emergency.data.local.entity.DbMessagePriority
import com.mesh.emergency.data.local.entity.DbMessageType

/**
 * Metadata container representing packet header properties.
 */
data class PacketHeader(
    val version: Int = 1,
    val packetId: String,
    val senderId: String,
    val receiverId: String,
    val messageType: DbMessageType,
    val priority: DbMessagePriority,
    val ttl: Long,
    val hopCount: Int = 0,
    val timestamp: Long
)
