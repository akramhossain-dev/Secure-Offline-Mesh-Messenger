/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.domain.model

import com.mesh.emergency.core.model.BaseModel

/**
 * Domain model representing paired peer contacts identity states.
 */
data class ContactIdentity(
    override val id: String,
    val trustedStatus: Boolean,
    val nickname: String,
    val lastCommunication: Long
) : BaseModel
