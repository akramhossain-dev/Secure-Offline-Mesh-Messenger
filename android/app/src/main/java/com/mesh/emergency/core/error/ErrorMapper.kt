/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.error

/**
 * Maps raw throwables and AppExceptions to structured domain Failures.
 */
object ErrorMapper {

    /**
     * Converts a [Throwable] into a domain [Failure].
     */
    fun map(throwable: Throwable): Failure {
        return when (throwable) {
            is AppException.Database -> Failure.Database(
                message = throwable.message ?: "Database read or write operation failed",
                cause = throwable.cause
            )
            is AppException.Bluetooth -> Failure.Bluetooth(
                message = throwable.message ?: "Bluetooth BLE connection failed",
                cause = throwable.cause
            )
            is AppException.LoRa -> Failure.LoRa(
                message = throwable.message ?: "LoRa mesh transmission failed",
                cause = throwable.cause
            )
            is AppException.Crypto -> Failure.Crypto(
                message = throwable.message ?: "Integrity signature verification failed",
                cause = throwable.cause
            )
            is AppException.Network -> Failure.Network(
                message = throwable.message ?: "Mesh routing handshake failed",
                cause = throwable.cause
            )
            is AppException.Validation -> Failure.Validation(
                message = throwable.message ?: "Input format validation check failed",
                cause = throwable.cause
            )
            is AppException -> Failure.Unknown(
                message = throwable.message ?: "Internal application exception encountered",
                cause = throwable
            )
            else -> Failure.Unknown(
                message = throwable.message ?: "An unexpected error occurred",
                cause = throwable
            )
        }
    }
}
