/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.domain.model

import com.mesh.emergency.core.model.BaseModel

/**
 * Domain model representing ESP32/LoRa network bridge node properties.
 */
data class NodeIdentity(
    override val id: String,
    val nodeType: String,
    val status: String,
    val lastSeen: Long,
    val signalPlaceholder: String?
) : BaseModel
