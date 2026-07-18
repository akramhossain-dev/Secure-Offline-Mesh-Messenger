/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.security

import com.mesh.emergency.core.security.KeyManager
import com.mesh.emergency.core.security.KeyStorage
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

/**
 * Implementation of [KeyManager] generating Elliptic Curve (secp256r1) keypairs.
 * Performs ECDH key agreements to derive shared secrets.
 *
 * Hardened to prevent insecure hardcoded fallback credentials (A34.3).
 */
@Singleton
class KeyManagerImpl @Inject constructor(
    private val keyStorage: KeyStorage
) : KeyManager {

    private val ALIAS_IDENTITY = "identity_key"

    override fun initializeIdentityKeys() {
        if (keyStorage.getPrivateKey(ALIAS_IDENTITY) != null) return

        try {
            val kpg = KeyPairGenerator.getInstance("EC")
            kpg.initialize(ECGenParameterSpec("secp256r1"))
            val kp = kpg.generateKeyPair()

            keyStorage.savePrivateKey(ALIAS_IDENTITY, kp.private.encoded)
            keyStorage.savePublicKey(ALIAS_IDENTITY, kp.public.encoded)
            Timber.d("KeyManager: Initialized new identity EC key pair")
        } catch (e: Exception) {
            Timber.e(e, "KeyManager: Failed to initialize identity keys")
            throw IllegalStateException("Failed to generate identity keys", e)
        }
    }

    override fun getIdentityPublicKey(): ByteArray {
        initializeIdentityKeys()
        return keyStorage.getPublicKey(ALIAS_IDENTITY)
            ?: throw IllegalStateException("Identity public key not found")
    }

    override fun deriveSharedSecret(theirPublicKey: ByteArray): ByteArray {
        initializeIdentityKeys()
        val privateKeyBytes = keyStorage.getPrivateKey(ALIAS_IDENTITY)
            ?: throw IllegalStateException("Identity private key not found")

        return try {
            val kf = KeyFactory.getInstance("EC")
            val privateKey = kf.generatePrivate(PKCS8EncodedKeySpec(privateKeyBytes))
            val publicKey = kf.generatePublic(X509EncodedKeySpec(theirPublicKey))

            val ka = KeyAgreement.getInstance("ECDH")
            ka.init(privateKey)
            ka.doPhase(publicKey, true)
            val rawSecret = ka.generateSecret()

            // Apply HKDF (RFC 5869) — HMAC-SHA256 extract then expand
            // Using raw ECDH output directly as AES key is a known cryptographic weakness.
            hkdfDerive(rawSecret, info = "mesh-e2e-key".toByteArray(), length = 32)
        } catch (e: Exception) {
            Timber.e(e, "KeyManager: ECDH derivation failed")
            throw IllegalArgumentException("Failed to derive shared secret", e)
        }
    }

    /**
     * HKDF (RFC 5869) using HMAC-SHA256.
     *
     * @param inputKeyMaterial raw ECDH shared secret
     * @param salt  optional salt — defaults to zero-filled SHA-256 block
     * @param info  context and application specific information (domain separation)
     * @param length  desired output key length in bytes (≤ 32 for SHA-256)
     */
    private fun hkdfDerive(
        inputKeyMaterial: ByteArray,
        salt: ByteArray = ByteArray(32), // zero-salt per RFC 5869 §2.2
        info: ByteArray,
        length: Int = 32
    ): ByteArray {
        // Step 1: Extract — HMAC-SHA256(salt, IKM)
        val prk = hmacSha256(salt, inputKeyMaterial)
        // Step 2: Expand — HMAC-SHA256(PRK, info || 0x01)
        val okm = hmacSha256(prk, info + byteArrayOf(0x01))
        return okm.copyOf(length)
    }

    private fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data)
    }
