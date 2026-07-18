/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.domain.repository

import com.mesh.emergency.core.common.result.Result
import com.mesh.emergency.core.model.BaseModel
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface defining user profile configurations access.
 */
interface UserRepository {
    /** Retrieves local user profile. */
    fun getCurrentUser(): Flow<Result<UserDomainModel>>

    /** Updates local profile details. */
    suspend fun updateProfile(username: String, avatarUrl: String?): Result<Unit>
}

/**
 * Domain model representing a local or remote user.
 */
data class UserDomainModel(
    override val id: String,
    val username: String,
    val publicKey: String,
    val avatarUrl: String?
) : BaseModel
