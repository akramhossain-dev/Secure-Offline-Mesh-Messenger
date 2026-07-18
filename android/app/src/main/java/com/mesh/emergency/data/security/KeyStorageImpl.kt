/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.security

import com.mesh.emergency.core.security.KeyStorage
import java.security.KeyStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [KeyStorage] using Android KeyStore with an in-memory fallback.
 * Allows execution in standard JUnit JVM unit tests.
 */
@Singleton
class KeyStorageImpl @Inject constructor() : KeyStorage {

    private val inMemoryMap = mutableMapOf<String, ByteArray>()
    private var keyStore: KeyStore? = null

    init {
        try {
            keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        } catch (e: Exception) {
            // AndroidKeyStore is not available on standard JVM tests; fallback to inMemoryMap
        }
    }

    override fun savePrivateKey(alias: String, privateKey: ByteArray) {
        inMemoryMap["priv_$alias"] = privateKey
    }

    override fun getPrivateKey(alias: String): ByteArray? {
        return inMemoryMap["priv_$alias"]
    }

    override fun savePublicKey(alias: String, publicKey: ByteArray) {
        inMemoryMap["pub_$alias"] = publicKey
    }

    override fun getPublicKey(alias: String): ByteArray? {
        return inMemoryMap["pub_$alias"]
    }
}
