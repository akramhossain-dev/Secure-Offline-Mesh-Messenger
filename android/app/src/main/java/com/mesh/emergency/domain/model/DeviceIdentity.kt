/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.domain.model

import com.mesh.emergency.core.model.BaseModel

/**
 * Domain model representing a device's identity parameters.
 */
data class DeviceIdentity(
    override val id: String,
    val deviceName: String,
    val deviceType: String,
    val platformInfo: String,
    val createdTime: Long,
    val lastActiveTime: Long
) : BaseModel
