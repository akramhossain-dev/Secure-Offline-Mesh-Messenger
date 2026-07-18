/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.domain.usecase.profile

import com.mesh.emergency.core.domain.usecase.SuspendUseCase
import com.mesh.emergency.di.IoDispatcher
import com.mesh.emergency.domain.repository.UserRepository
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

/**
 * Use Case to update local profile properties in local database records.
 */
class UpdateProfileUseCase @Inject constructor(
    private val userRepository: UserRepository,
    @IoDispatcher dispatcher: CoroutineDispatcher
) : SuspendUseCase<UpdateProfileUseCase.Params, Unit>(dispatcher) {

    override suspend fun execute(params: Params) {
        val result = userRepository.updateProfile(params.username, params.avatarUrl)
        if (result is com.mesh.emergency.core.common.result.Result.Error) {
            throw result.exception
        }
    }

    /** Inputs parameters. */
    data class Params(val username: String, val avatarUrl: String?)
}
