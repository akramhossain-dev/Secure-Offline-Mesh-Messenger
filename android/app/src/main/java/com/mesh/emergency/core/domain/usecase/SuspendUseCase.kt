/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.domain.usecase

import com.mesh.emergency.core.common.result.Result
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Base Use Case encapsulating a single suspendable transactional business operation.
 * Automatically runs on the provided [CoroutineDispatcher] background thread and maps exceptions.
 */
abstract class SuspendUseCase<in Params, out Type>(
    private val dispatcher: CoroutineDispatcher
) : BaseUseCase {

    /**
     * Executes the use case within the defined [dispatcher] context.
     * Catches and wraps failures into a [Result.Error].
     */
    suspend operator fun invoke(params: Params): Result<Type> {
        return withContext(dispatcher) {
            try {
                Result.Success(execute(params))
            } catch (e: Throwable) {
                Result.Error(e)
            }
        }
    }

    /**
     * Target execution block containing business logic.
     */
    protected abstract suspend fun execute(params: Params): Type
}
