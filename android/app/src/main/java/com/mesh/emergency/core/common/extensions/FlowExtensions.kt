/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.common.extensions

import com.mesh.emergency.core.common.result.Result
import com.mesh.emergency.core.common.result.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

// ─────────────────────────────────────────────────────────────────────────────
// Flow Extension Functions
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Wraps a [Flow] of [T] in a [Flow] of [Result]<[T]>.
 *
 * - Emits [Result.Loading] immediately on collection start
 * - Maps each emitted value to [Result.Success]
 * - Catches any exception and emits [Result.Error]
 *
 * Usage:
 * ```kotlin
 * messageDao.getAll()
 *     .asResult()
 *     .collect { result ->
 *         when (result) {
 *             is Result.Loading -> showLoading()
 *             is Result.Success -> showData(result.data)
 *             is Result.Error   -> showError(result.message)
 *         }
 *     }
 * ```
 */
fun <T> Flow<T>.asResult(): Flow<Result<T>> =
    this
        .map<T, Result<T>> { Result.Success(it) }
        .onStart { emit(Result.Loading) }
        .catch { emit(Result.Error(it)) }

/**
 * Maps each emission of [Result.Success] using [transform],
 * passing [Result.Loading] and [Result.Error] through unchanged.
 */
inline fun <T, R> Flow<Result<T>>.mapResult(
    crossinline transform: (T) -> R,
): Flow<Result<R>> = map { result -> result.map(transform) }
