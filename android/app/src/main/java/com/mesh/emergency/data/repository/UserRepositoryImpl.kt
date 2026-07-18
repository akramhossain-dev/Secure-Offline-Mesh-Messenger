/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.repository

import com.mesh.emergency.core.common.result.Result
import com.mesh.emergency.data.local.LocalDataSource
import com.mesh.emergency.data.local.entity.UserEntity
import com.mesh.emergency.domain.repository.UserDomainModel
import com.mesh.emergency.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository regulating user profile configurations operations using local database.
 */
@Singleton
class UserRepositoryImpl @Inject constructor(
    private val localDataSource: LocalDataSource
) : UserRepository {

    override fun getCurrentUser(): Flow<Result<UserDomainModel>> {
        return localDataSource.getCurrentUser().map { entity ->
            if (entity != null) {
                Result.Success(entity.toDomain())
            } else {
                Result.Error(Exception("Local user profile has not been initialized"))
            }
        }
    }

    override suspend fun updateProfile(username: String, avatarUrl: String?): Result<Unit> {
        return try {
            val existing = localDataSource.getUserById("local_user_id")
            val now = System.currentTimeMillis()
            val user = UserEntity(
                entityId = "local_user_id",
                username = username,
                profileImageRef = avatarUrl,
                languagePreference = existing?.languagePreference ?: "en",
                createdTime = existing?.createdTime ?: now,
                updatedTime = now,
                status = "ACTIVE",
                isCurrentUser = true,
                lastSeen = now,
                publicKey = existing?.publicKey ?: "00000000000000000000"
            )
            localDataSource.insertUser(user)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}

private fun UserEntity.toDomain(): UserDomainModel = UserDomainModel(
    id = entityId,
    username = username,
    publicKey = publicKey,
    avatarUrl = profileImageRef
)
