/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.domain.usecase.profile

import com.mesh.emergency.core.domain.usecase.SuspendUseCase
import com.mesh.emergency.data.local.LocalDataSource
import com.mesh.emergency.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

/**
 * Use Case to remove user profile details and clean local database registries.
 */
class DeleteProfileUseCase @Inject constructor(
    private val localDataSource: LocalDataSource,
    @IoDispatcher dispatcher: CoroutineDispatcher
) : SuspendUseCase<Unit, Unit>(dispatcher) {

    override suspend fun execute(params: Unit) {
        val user = localDataSource.getUserById("local_user_id")
        if (user != null) {
            localDataSource.deleteUser(user)
        }
    }
}
