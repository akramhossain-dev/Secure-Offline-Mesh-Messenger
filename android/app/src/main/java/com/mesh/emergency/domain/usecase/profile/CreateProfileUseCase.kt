/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.domain.usecase.profile

import com.mesh.emergency.core.domain.usecase.SuspendUseCase
import com.mesh.emergency.core.identity.IdentityGenerator
import com.mesh.emergency.di.IoDispatcher
import com.mesh.emergency.domain.repository.UserRepository
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

/**
 * Use Case to initialize a new user profile with custom name and avatar reference.
 * Automatically generates fallback names using device identifiers if blank.
 */
class CreateProfileUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val identityGenerator: IdentityGenerator,
    @IoDispatcher dispatcher: CoroutineDispatcher
) : SuspendUseCase<CreateProfileUseCase.Params, Unit>(dispatcher) {

    override suspend fun execute(params: Params) {
        val username = params.username.ifBlank {
            "Mesh_${identityGenerator.generateFingerprint().take(6)}"
        }
        val result = userRepository.updateProfile(username, params.avatarUrl)
        if (result is com.mesh.emergency.core.common.result.Result.Error) {
            throw result.exception
        }
    }

    /** Inputs parameters. */
    data class Params(val username: String, val avatarUrl: String?)
}
