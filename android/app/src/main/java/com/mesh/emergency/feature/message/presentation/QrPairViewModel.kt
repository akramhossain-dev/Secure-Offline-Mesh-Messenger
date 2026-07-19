/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.feature.message.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mesh.emergency.core.discovery.qr.QRHandshakeData
import com.mesh.emergency.core.discovery.qr.QRHandshakeManager
import com.mesh.emergency.data.local.LocalDataSource
import com.mesh.emergency.data.local.entity.DbTrustStatus
import com.mesh.emergency.data.local.entity.DeviceEntity
import com.mesh.emergency.data.local.entity.UserEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel managing the QR pairing/scanning logic.
 * Decodes the public key handshakes and updates local DB entities.
 */
@HiltViewModel
class QrPairViewModel @Inject constructor(
    private val localDataSource: LocalDataSource
) : ViewModel() {

    private val _effect = MutableSharedFlow<QrPairUiEffect>()
    val effect: SharedFlow<QrPairUiEffect> = _effect.asSharedFlow()

    /**
     * Decodes QR code handshake JSON string and persists the contact and device profiles.
     */
    fun processHandshakePayload(payload: String) {
        viewModelScope.launch {
            try {
                // 1. Decode handshake JSON
                val data = QRHandshakeManager.parsePayload(payload)

                // 2. Map and insert User (Contact) Entity
                val user = UserEntity(
                    entityId = data.userId,
                    username = "Contact-${data.userId.take(6)}",
                    profileImageRef = null,
                    languagePreference = "en",
                    createdTime = System.currentTimeMillis(),
                    updatedTime = System.currentTimeMillis(),
                    status = "ACTIVE",
                    isCurrentUser = false,
                    lastSeen = System.currentTimeMillis(),
                    trustedStatus = true,
                    nickname = "Paired Partner (${data.deviceType})"
                )

                // 3. Map and insert Device Entity
                val device = DeviceEntity(
                    entityId = data.deviceId,
                    name = "Device-${data.deviceType}",
                    rssi = -55,
                    lastSeen = System.currentTimeMillis(),
                    deviceType = data.deviceType,
                    platformInfo = "ANDROID",
                    createdTime = System.currentTimeMillis(),
                    lastActiveTime = System.currentTimeMillis(),
                    trustStatus = DbTrustStatus.TRUSTED,
                    nickname = "Paired Node"
                )

                localDataSource.insertUser(user)
                localDataSource.insertDevice(device)

                _effect.emit(QrPairUiEffect.Success("Contact paired successfully!"))
            } catch (e: Exception) {
                _effect.emit(QrPairUiEffect.Error("Invalid pairing payload: ${e.message}"))
            }
        }
    }

    /**
     * Generates a simulated JSON handshake string and processes it.
     */
    fun simulateScan() {
        val mockUserId = UUID.randomUUID().toString()
        val mockDeviceId = UUID.randomUUID().toString()
        val mockData = QRHandshakeData(
            version = 1,
            deviceId = mockDeviceId,
            userId = mockUserId,
            deviceType = "SMARTPHONE",
            publicKeyRef = "04" + UUID.randomUUID().toString().replace("-", ""),
            timestamp = System.currentTimeMillis()
        )
        val payload = QRHandshakeManager.generatePayload(mockData)
        processHandshakePayload(payload)
    }
}

sealed interface QrPairUiEffect {
    data class Success(val message: String) : QrPairUiEffect
    data class Error(val message: String) : QrPairUiEffect
}
