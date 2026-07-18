/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.security

/**
 * Interface contract protecting secrets and keys in local storage.
 */
interface KeyStorage {
    /** Saves binary private keys. */
    fun savePrivateKey(alias: String, privateKey: ByteArray)

    /** Retrieves binary private keys. */
    fun getPrivateKey(alias: String): ByteArray?

    /** Saves binary public keys. */
    fun savePublicKey(alias: String, publicKey: ByteArray)

    /** Retrieves binary public keys. */
    fun getPublicKey(alias: String): ByteArray?

    /** Purges all stored keys and metadata. */
    fun clearAll()
}
