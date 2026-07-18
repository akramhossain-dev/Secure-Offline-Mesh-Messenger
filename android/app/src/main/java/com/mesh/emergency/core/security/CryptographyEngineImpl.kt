/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.security

import com.mesh.emergency.core.common.result.Result
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [CryptographyEngine] executing standard JCE cryptography operations.
 * Portably runs on standard JVM and Android runtime layers.
 */
@Singleton
class CryptographyEngineImpl @Inject constructor() : CryptographyEngine {

    private val GCM_IV_LENGTH = 12
    private val GCM_TAG_LENGTH = 128 // bits
    private val secureRandom = SecureRandom()

    override fun encrypt(plainText: ByteArray, secretKey: ByteArray): Result<EncryptedData> {
        return try {
            val iv = ByteArray(GCM_IV_LENGTH)
            secureRandom.nextBytes(iv)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val keySpec = SecretKeySpec(secretKey, "AES")
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)

            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)
            val cipherText = cipher.doFinal(plainText)

            Result.Success(EncryptedData(cipherText, iv, null))
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override fun decrypt(cipherText: ByteArray, secretKey: ByteArray, iv: ByteArray): Result<ByteArray> {
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val keySpec = SecretKeySpec(secretKey, "AES")
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)

            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)
            val plainText = cipher.doFinal(cipherText)

            Result.Success(plainText)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override fun hashSha256(data: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(data)
    }
}
