/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency

import com.mesh.emergency.core.audio.AudioInfo
import com.mesh.emergency.core.audio.AudioProvider
import com.mesh.emergency.core.common.result.Result
import com.mesh.emergency.core.communication.CommunicationManager
import com.mesh.emergency.core.communication.DeliveryResult
import com.mesh.emergency.data.audio.VoiceManagerImpl
import com.mesh.emergency.data.local.LocalDataSource
import com.mesh.emergency.core.identity.DeviceFingerprintProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.any
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations

/**
 * Unit test validating VoiceManager metadata caching and audio provider integrations.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class VoiceManagerTest {

    @Mock
    private lateinit var mockLocalDataSource: LocalDataSource

    @Mock
    private lateinit var mockAudioProvider: AudioProvider

    @Mock
    private lateinit var mockCommunicationManager: CommunicationManager

    @Mock
    private lateinit var mockDeviceFingerprintProvider: DeviceFingerprintProvider

    private lateinit var voiceManager: VoiceManagerImpl

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        `when`(mockDeviceFingerprintProvider.getDeviceFingerprint()).thenReturn("local_user_id")
        voiceManager = VoiceManagerImpl(
            mockLocalDataSource,
            mockAudioProvider,
            mockCommunicationManager,
            mockDeviceFingerprintProvider
        )
    }

    @Test
    fun testRecordAndSendVoice_createsMetadataAndTriggersBroadcast() = runTest {
        val info = AudioInfo(
            filePath = "/storage/emulated/0/audio/voice_stub.opus",
            durationMs = 5000,
            fileSize = 4096,
            format = "opus"
        )

        `when`(mockAudioProvider.startRecording()).thenReturn(Result.Success(Unit))
        `when`(mockAudioProvider.stopRecording()).thenReturn(Result.Success(info))
        `when`(mockCommunicationManager.sendMessage(anyByteArray())).thenReturn(
            Result.Success(DeliveryResult.SENT)
        )

        val result = voiceManager.recordAndSendVoice("user_recipient_id", "EMERGENCY_QUALITY")
        assertTrue(result is Result.Success)

        val voice = (result as Result.Success).data
        assertEquals("/storage/emulated/0/audio/voice_stub.opus", voice.fileReference)
        assertEquals(5000L, voice.duration)
        assertEquals(4096L, voice.fileSize)
        assertEquals("opus", voice.format)
        assertEquals("EMERGENCY_QUALITY", voice.quality)

        verify(mockAudioProvider).startRecording()
        verify(mockAudioProvider).stopRecording()
        verify(mockLocalDataSource).insertVoiceMessage(anyVoiceMessageEntity())
        verify(mockCommunicationManager).sendMessage(anyByteArray())
    }
}

private fun anyByteArray(): ByteArray {
    org.mockito.Mockito.any(ByteArray::class.java)
    return ByteArray(0)
}

private fun anyVoiceMessageEntity(): com.mesh.emergency.data.local.entity.VoiceMessageEntity {
    org.mockito.Mockito.any(com.mesh.emergency.data.local.entity.VoiceMessageEntity::class.java)
    return com.mesh.emergency.data.local.entity.VoiceMessageEntity(
        entityId = "",
        senderId = "",
        receiverId = "",
        fileReference = "",
        duration = 0L,
        fileSize = 0L,
        format = "",
        quality = "",
        timestamp = 0L,
        status = ""
    )
}
