/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.error

/**
 * Domain level failures representing user-friendly states suitable for presentation.
 */
sealed interface Failure {
    /** Brief user-facing error description. */
    val message: String

    /** Local database layer failed. */
    data class Database(override val message: String, val cause: Throwable? = null) : Failure

    /** BLE central or server operation failed. */
    data class Bluetooth(override val message: String, val cause: Throwable? = null) : Failure

    /** LoRa packet broadcast or confirmation timed out. */
    data class LoRa(override val message: String, val cause: Throwable? = null) : Failure

    /** Decryption key mismatches or integrity validations failed. */
    data class Crypto(override val message: String, val cause: Throwable? = null) : Failure

    /** Connection or handshake with a peer failed. */
    data class Network(override val message: String, val cause: Throwable? = null) : Failure

    /** Input data bounds check failed. */
    data class Validation(override val message: String, val cause: Throwable? = null) : Failure

    /** General errors. */
    data class Unknown(override val message: String, val cause: Throwable? = null) : Failure
}
