/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.security

/**
 * Interface contract managing cryptographic keys generation and Diffie-Hellman exchanges.
 */
interface KeyManager {
    /** Triggers key generation and stores device identity keypair. */
    fun initializeIdentityKeys()

    /** Returns binary representation of device public key. */
    fun getIdentityPublicKey(): ByteArray

    /**
     * Derives a shared AES-256 secret key from our private key and their public key.
     */
    fun deriveSharedSecret(theirPublicKey: ByteArray): ByteArray
}
