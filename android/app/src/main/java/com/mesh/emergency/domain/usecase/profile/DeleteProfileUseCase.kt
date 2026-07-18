/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.domain.usecase.profile

import com.mesh.emergency.core.domain.usecase.SuspendUseCase
import com.mesh.emergency.core.identity.DeviceFingerprintProvider
import com.mesh.emergency.data.local.LocalDataSource
import com.mesh.emergency.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

/**
 * Use Case to remove user profile details and clean local database registries.
 */
class DeleteProfileUseCase @Inject constructor(
    private val localDataSource: LocalDataSource,
    private val deviceFingerprintProvider: DeviceFingerprintProvider,
    @IoDispatcher dispatcher: CoroutineDispatcher
) : SuspendUseCase<Unit, Unit>(dispatcher) {

    override suspend fun execute(params: Unit) {
        val userId = deviceFingerprintProvider.getDeviceFingerprint()
        val user = localDataSource.getUserById(userId)
        if (user != null) {
            localDataSource.deleteUser(user)
        }
    }
}
