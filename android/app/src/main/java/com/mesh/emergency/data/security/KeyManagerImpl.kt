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
            ka.generateSecret()
        } catch (e: Exception) {
            Timber.e(e, "KeyManager: ECDH derivation failed")
            throw IllegalArgumentException("Failed to derive shared secret", e)
        }
    }
}
