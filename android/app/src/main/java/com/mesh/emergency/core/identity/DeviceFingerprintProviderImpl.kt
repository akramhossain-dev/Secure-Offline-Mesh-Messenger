/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.identity

import android.content.Context
import android.os.Build
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [DeviceFingerprintProvider] computing SHA-256 signatures of non-sensitive parameters.
 */
@Singleton
class DeviceFingerprintProviderImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : DeviceFingerprintProvider {

    override fun getDeviceFingerprint(): String {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: ""
        val hardwareInfo = "${Build.BRAND}-${Build.MODEL}-${Build.HARDWARE}"
        val rawString = "$androidId-$hardwareInfo"
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(rawString.toByteArray(Charsets.UTF_8))
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            // Fallback to simple hashcode if message digest fails
            rawString.hashCode().toString()
        }
    }
}
