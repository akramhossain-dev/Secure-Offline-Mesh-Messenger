/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.repository

import com.mesh.emergency.core.identity.DeviceFingerprintProvider
import com.mesh.emergency.core.common.result.Result
import com.mesh.emergency.core.security.KeyManager
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
    private val localDataSource: LocalDataSource,
    private val deviceFingerprintProvider: DeviceFingerprintProvider,
    private val keyManager: KeyManager
) : UserRepository {

    override fun getCurrentUser(): Flow<Result<UserDomainModel>> {
        return localDataSource.getCurrentUser().map { entity ->
            if (entity != null) {
                val pubKeyHex = try {
                    keyManager.getIdentityPublicKey().joinToString("") { "%02x".format(it) }
                } catch (e: Exception) {
                    "00000000000000000000"
                }
                Result.Success(entity.toDomain(pubKeyHex))
            } else {
                Result.Error(Exception("Local user profile has not been initialized"))
            }
        }
    }

    override suspend fun updateProfile(username: String, avatarUrl: String?): Result<Unit> {
        return try {
            val userId = deviceFingerprintProvider.getDeviceFingerprint()
            val existing = localDataSource.getUserById(userId)
            val now = System.currentTimeMillis()
            val user = UserEntity(
                entityId = userId,
                username = username,
                profileImageRef = avatarUrl,
                languagePreference = existing?.languagePreference ?: "en",
                createdTime = existing?.createdTime ?: now,
                updatedTime = now,
                status = "ACTIVE",
                isCurrentUser = true,
                lastSeen = now
            )
            localDataSource.insertUser(user)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}

private fun UserEntity.toDomain(publicKey: String): UserDomainModel = UserDomainModel(
    id = entityId,
    username = username,
    publicKey = publicKey,
    avatarUrl = profileImageRef
)
