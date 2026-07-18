/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency

import com.mesh.emergency.core.common.result.Result
import com.mesh.emergency.core.security.CryptographyEngineImpl
import com.mesh.emergency.data.security.KeyManagerImpl
import com.mesh.emergency.data.security.KeyStorageImpl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit test validating AES-256-GCM encryption and ECDH shared keys derivation.
 */
class SecurityLayerTest {

    private lateinit var cryptoEngine: CryptographyEngineImpl
    private lateinit var keyStorage: KeyStorageImpl
    private lateinit var keyManager: KeyManagerImpl

    @Before
    fun setUp() {
        cryptoEngine = CryptographyEngineImpl()
        keyStorage = KeyStorageImpl()
        keyManager = KeyManagerImpl(keyStorage)
    }

    @Test
    fun testAesGcm_encryptionDecryptionRoundtrip() {
        val key = ByteArray(32) { 1 } // 256-bit AES key
        val plaintext = "hello secure mesh network".toByteArray()

        val encryptResult = cryptoEngine.encrypt(plaintext, key)
        assertTrue(encryptResult is Result.Success)

        val encryptedData = (encryptResult as Result.Success).data
        val decryptResult = cryptoEngine.decrypt(encryptedData.cipherText, key, encryptedData.iv)
        assertTrue(decryptResult is Result.Success)

        val decrypted = (decryptResult as Result.Success).data
        assertEquals("hello secure mesh network", String(decrypted))
    }

    @Test
    fun testDeriveSharedSecret_createsMatchingSecrets() {
        val storageAlice = KeyStorageImpl()
        val managerAlice = KeyManagerImpl(storageAlice)

        val storageBob = KeyStorageImpl()
        val managerBob = KeyManagerImpl(storageBob)

        val pubAlice = managerAlice.getIdentityPublicKey()
        val pubBob = managerBob.getIdentityPublicKey()

        val secretAlice = managerAlice.deriveSharedSecret(pubBob)
        val secretBob = managerBob.deriveSharedSecret(pubAlice)

        assertEquals(secretAlice.size, secretBob.size)
        // Verify both sides derived the identical session key
        assertTrue(secretAlice.contentEquals(secretBob))
    }

    @Test
    fun testHashSha256_returnsUniqueValue() {
        val hash1 = cryptoEngine.hashSha256("input 1".toByteArray())
        val hash2 = cryptoEngine.hashSha256("input 2".toByteArray())

        assertNotEquals(hash1.joinToString(""), hash2.joinToString(""))
    }
}
