/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.domain.usecase.profile

import com.mesh.emergency.core.common.result.Result
import com.mesh.emergency.core.domain.usecase.FlowUseCase
import com.mesh.emergency.di.IoDispatcher
import com.mesh.emergency.domain.repository.UserDomainModel
import com.mesh.emergency.domain.repository.UserRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use Case retrieving the local user profile updates stream.
 */
class GetProfileUseCase @Inject constructor(
    private val userRepository: UserRepository,
    @IoDispatcher dispatcher: CoroutineDispatcher
) : FlowUseCase<Unit, UserDomainModel>(dispatcher) {

    override fun execute(params: Unit): Flow<Result<UserDomainModel>> {
        return userRepository.getCurrentUser()
    }
}
