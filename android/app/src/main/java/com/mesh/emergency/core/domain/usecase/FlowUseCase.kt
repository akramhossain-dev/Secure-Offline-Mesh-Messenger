/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.domain.usecase

import com.mesh.emergency.core.common.result.Result
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn

/**
 * Base Use Case encapsulating a reactive stream of updates emitting [Result] states.
 */
abstract class FlowUseCase<in Params, out Type>(
    private val dispatcher: CoroutineDispatcher
) : BaseUseCase {

    /**
     * Returns a reactive flow of [Result] states executing on the provided [dispatcher].
     */
    operator fun invoke(params: Params): Flow<Result<Type>> {
        return execute(params)
            .flowOn(dispatcher)
    }

    /**
     * Target stream execution block emitting result packages.
     */
    protected abstract fun execute(params: Params): Flow<Result<Type>>
}
