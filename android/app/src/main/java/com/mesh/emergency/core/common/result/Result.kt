/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.common.result

/**
 * A discriminated union that encapsulates a successful outcome with a value of type [T],
 * a loading state, or a failure with an arbitrary [Throwable] exception.
 *
 * Design rationale:
 * - Avoids try/catch scattered across the codebase
 * - Makes loading state explicit in the type system
 * - Composable with Kotlin's functional operators
 * - Used as the return type from all Repository and UseCase functions
 *
 * Usage examples:
 * ```kotlin
 * // Repository
 * suspend fun getMessages(): Result<List<Message>> = runCatching {
 *     messageDao.getAll().map { it.toDomain() }
 * }.fold(
 *     onSuccess = { Result.Success(it) },
 *     onFailure = { Result.Error(it) }
 * )
 *
 * // ViewModel / UseCase consumer
 * when (val result = getMessagesUseCase()) {
 *     is Result.Loading -> showLoading()
 *     is Result.Success -> displayMessages(result.data)
 *     is Result.Error   -> showError(result.exception.message)
 * }
 * ```
 */
sealed class Result<out T> {

    /** Represents a successful computation with a [data] value. */
    data class Success<T>(val data: T) : Result<T>()

    /** Represents a loading / in-progress state. */
    data object Loading : Result<Nothing>()

    /** Represents a failed computation with an [exception]. */
    data class Error(
        val exception: Throwable,
        val message: String = exception.message ?: "An unknown error occurred",
    ) : Result<Nothing>()

    // ─────────────────────────────────────────────────────────────────────────
    // Convenience properties
    // ─────────────────────────────────────────────────────────────────────────

    /** `true` if this is [Success]. */
    val isSuccess: Boolean get() = this is Success

    /** `true` if this is [Error]. */
    val isError: Boolean get() = this is Error

    /** `true` if this is [Loading]. */
    val isLoading: Boolean get() = this is Loading

    /** Returns the [Success.data] value or `null` if not [Success]. */
    fun getOrNull(): T? = (this as? Success)?.data

    /** Returns the [Error.exception] or `null` if not [Error]. */
    fun exceptionOrNull(): Throwable? = (this as? Error)?.exception
}

// ─────────────────────────────────────────────────────────────────────────────
// Extension functions
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Maps the [Result.Success] data value using [transform].
 * [Result.Loading] and [Result.Error] pass through unchanged.
 */
inline fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> = when (this) {
    is Result.Success -> Result.Success(transform(data))
    is Result.Loading -> Result.Loading
    is Result.Error   -> Result.Error(exception, message)
}

/**
 * Executes [onSuccess] if this is [Result.Success],
 * [onError] if this is [Result.Error],
 * [onLoading] if this is [Result.Loading].
 */
inline fun <T> Result<T>.fold(
    onSuccess: (T) -> Unit = {},
    onError: (Throwable) -> Unit = {},
    onLoading: () -> Unit = {},
) {
    when (this) {
        is Result.Success -> onSuccess(data)
        is Result.Error   -> onError(exception)
        is Result.Loading -> onLoading()
    }
}

/**
 * Wraps the [block] execution in a [Result], catching any [Exception] as [Result.Error].
 */
inline fun <T> resultOf(block: () -> T): Result<T> = try {
    Result.Success(block())
} catch (e: Exception) {
    Result.Error(e)
}
