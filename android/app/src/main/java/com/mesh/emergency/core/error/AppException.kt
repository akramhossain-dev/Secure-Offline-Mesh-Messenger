/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.error

/**
 * Custom runtime exceptions thrown by database or communications layers.
 */
sealed class AppException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause) {

    /** Database persistence exception. */
    class Database(message: String, cause: Throwable? = null) : AppException(message, cause)

    /** Bluetooth communication interface exception. */
    class Bluetooth(message: String, cause: Throwable? = null) : AppException(message, cause)

    /** LoRa communication routing boundary exception. */
    class LoRa(message: String, cause: Throwable? = null) : AppException(message, cause)

    /** Cipher or signature key validation exception. */
    class Crypto(message: String, cause: Throwable? = null) : AppException(message, cause)

    /** Peer data sync exception. */
    class Network(message: String, cause: Throwable? = null) : AppException(message, cause)

    /** UI inputs validation constraint error. */
    class Validation(message: String, cause: Throwable? = null) : AppException(message, cause)

    /** Catch-all fallback exception wrapper. */
    class Unknown(message: String, cause: Throwable? = null) : AppException(message, cause)
}
