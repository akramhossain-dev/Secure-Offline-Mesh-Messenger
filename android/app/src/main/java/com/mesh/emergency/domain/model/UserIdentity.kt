/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.domain.model

import com.mesh.emergency.core.model.BaseModel

/**
 * Domain model representing a user's persistent profile metadata.
 */
data class UserIdentity(
    override val id: String,
    val displayName: String,
    val profileImageRef: String?,
    val languagePreference: String,
    val createdTime: Long,
    val updatedTime: Long,
    val status: String
) : BaseModel
