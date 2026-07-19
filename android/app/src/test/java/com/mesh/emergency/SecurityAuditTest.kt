/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency

import com.mesh.emergency.core.security.CryptographyEngineImpl
import com.mesh.emergency.data.security.KeyManagerImpl
import com.mesh.emergency.data.security.KeyStorageImpl
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.security.SecureRandom

/**
 * Security audit and cryptographic verification tests (A34.10).
 * Validates GCM encryption, KeyStore operations, key rotation, and ECDH derivation.
 */
class SecurityAuditTest {

    private lateinit var cryptographyEngine: CryptographyEngineImpl
    private lateinit var keyStorage: KeyStorageImpl
    private lateinit var keyManager: KeyManagerImpl
    private val random = SecureRandom()

    @Before
    fun setup() {
        cryptographyEngine = CryptographyEngineImpl()
        keyStorage = KeyStorageImpl(null) // Pass null context to force JVM memory fallback
        keyManager = KeyManagerImpl(keyStorage)
    }

    @Test
    fun `GCM encryption and decryption round-trip succeeds`() {
        val secretKey = ByteArray(32).apply { random.nextBytes(this) }
        val plainText = "Secure Offline Mesh Message payload".toByteArray()

        val encryptResult = cryptographyEngine.encrypt(plainText, secretKey)
        assertTrue(encryptResult is com.mesh.emergency.core.common.result.Result.Success)

        val encryptedData = (encryptResult as com.mesh.emergency.core.common.result.Result.Success).data
        assertNotNull(encryptedData.cipherText)
        assertNotNull(encryptedData.iv)

        val decryptResult = cryptographyEngine.decrypt(
            encryptedData.cipherText,
            secretKey,
            encryptedData.iv
        )
        assertTrue(decryptResult is com.mesh.emergency.core.common.result.Result.Success)
        val decryptedText = (decryptResult as com.mesh.emergency.core.common.result.Result.Success).data

        assertArrayEquals(plainText, decryptedText)
    }

    @Test
    fun `GCM decryption fails with incorrect key`() {
        val keyA = ByteArray(32).apply { random.nextBytes(this) }
        val keyB = ByteArray(32).apply { random.nextBytes(this) }
        val plainText = "Top Secret".toByteArray()

        val encrypted = (cryptographyEngine.encrypt(plainText, keyA) as com.mesh.emergency.core.common.result.Result.Success).data

        val decryptResult = cryptographyEngine.decrypt(encrypted.cipherText, keyB, encrypted.iv)
        assertTrue(decryptResult is com.mesh.emergency.core.common.result.Result.Error)
    }

    @Test
    fun `GCM decryption fails with modified ciphertext`() {
        val secretKey = ByteArray(32).apply { random.nextBytes(this) }
        val plainText = "Top Secret".toByteArray()

        val encrypted = (cryptographyEngine.encrypt(plainText, secretKey) as com.mesh.emergency.core.common.result.Result.Success).data
        
        // Mutate ciphertext to violate integrity tag
        encrypted.cipherText[0] = (encrypted.cipherText[0].toInt() xor 1).toByte()

        val decryptResult = cryptographyEngine.decrypt(encrypted.cipherText, secretKey, encrypted.iv)
        assertTrue(decryptResult is com.mesh.emergency.core.common.result.Result.Error)
    }

    @Test
    fun `key storage fallback correctly stores and retrieves private keys`() {
        val alias = "test_key_alias"
        val privateKey = ByteArray(32).apply { random.nextBytes(this) }

        keyStorage.savePrivateKey(alias, privateKey)
        val retrieved = keyStorage.getPrivateKey(alias)

        assertNotNull(retrieved)
        assertArrayEquals(privateKey, retrieved)
    }

    @Test
    fun `key storage clearAll purges all saved keys`() {
        val alias = "rotate_key"
        val privateKey = ByteArray(32).apply { random.nextBytes(this) }
        val publicKey = ByteArray(32).apply { random.nextBytes(this) }

        keyStorage.savePrivateKey(alias, privateKey)
        keyStorage.savePublicKey(alias, publicKey)

        keyStorage.clearAll()

        assertNull(keyStorage.getPrivateKey(alias))
        assertNull(keyStorage.getPublicKey(alias))
    }

    @Test
    fun `key manager generates identity public key and derives shared secret`() {
        // Instantiate manager B to derive a shared secret
        val storageB = KeyStorageImpl(null)
        val managerB = KeyManagerImpl(storageB)

        val pubKeyA = keyManager.getIdentityPublicKey()
        val pubKeyB = managerB.getIdentityPublicKey()

        assertNotNull(pubKeyA)
        assertNotNull(pubKeyB)

        val secretDerivationA = keyManager.deriveSharedSecret(pubKeyB)
        val secretDerivationB = managerB.deriveSharedSecret(pubKeyA)

        assertNotNull(secretDerivationA)
        assertNotNull(secretDerivationB)
        
        // ECDH properties guarantee both derived secrets are identical
        assertArrayEquals(secretDerivationA, secretDerivationB)
    }
}
