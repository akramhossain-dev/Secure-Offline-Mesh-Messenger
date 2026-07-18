/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.repository

import com.mesh.emergency.core.common.result.Result
import com.mesh.emergency.domain.repository.UserDomainModel
import com.mesh.emergency.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data layer implementation of [UserRepository] returning stubbed values.
 */
@Singleton
class UserRepositoryImpl @Inject constructor() : UserRepository {

    override fun getCurrentUser(): Flow<Result<UserDomainModel>> {
        return flowOf(
            Result.Success(
                UserDomainModel(
                    id = "local_user_id",
                    username = "MeshResponder",
                    publicKey = "00000000000000000000",
                    avatarUrl = null
                )
            )
        )
    }

    override suspend fun updateProfile(username: String, avatarUrl: String?): Result<Unit> {
        return Result.Success(Unit)
    }
}
