/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.security

import com.mesh.emergency.core.common.result.Result

/**
 * Interface contract executing hashing and symmetric encryption algorithms.
 */
interface CryptographyEngine {
    /** Encrypts plain text bytes using AES-256-GCM. */
    fun encrypt(plainText: ByteArray, secretKey: ByteArray): Result<EncryptedData>

    /** Decrypts cipher text bytes using AES-256-GCM. */
    fun decrypt(cipherText: ByteArray, secretKey: ByteArray, iv: ByteArray): Result<ByteArray>

    /** Computes SHA-256 hash. */
    fun hashSha256(data: ByteArray): ByteArray
}

/**
 * Output structure for authenticated GCM encryption.
 */
data class EncryptedData(
    val cipherText: ByteArray,
    val iv: ByteArray,
    val authenticationTag: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EncryptedData) return false
        if (!cipherText.contentEquals(other.cipherText)) return false
        if (!iv.contentEquals(other.iv)) return false
        if (authenticationTag != null) {
            if (other.authenticationTag == null) return false
            if (!authenticationTag.contentEquals(other.authenticationTag)) return false
        } else if (other.authenticationTag != null) return false
        return true
    }

    override fun hashCode(): Int {
        var result = cipherText.contentHashCode()
        result = 31 * result + iv.contentHashCode()
        result = 31 * result + (authenticationTag?.contentHashCode() ?: 0)
        return result
    }
}
