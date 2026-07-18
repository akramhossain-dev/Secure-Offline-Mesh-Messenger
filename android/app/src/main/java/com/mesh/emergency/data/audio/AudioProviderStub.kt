/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.audio

import com.mesh.emergency.core.audio.AudioInfo
import com.mesh.emergency.core.audio.AudioProvider
import com.mesh.emergency.core.common.result.Result
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation stub of [AudioProvider] simulating microphone recording hooks.
 */
@Singleton
class AudioProviderStub @Inject constructor() : AudioProvider {

    override fun startRecording(): Result<Unit> {
        return Result.Success(Unit)
    }

    override fun stopRecording(): Result<AudioInfo> {
        return Result.Success(
            AudioInfo(
                filePath = "/storage/emulated/0/audio/voice_stub.opus",
                durationMs = 5000,
                fileSize = 4096,
                format = "opus"
            )
        )
    }

    override fun saveAudio(fileBytes: ByteArray, filename: String): Result<String> {
        return Result.Success("/storage/emulated/0/audio/$filename")
    }

    override fun deleteAudio(filePath: String): Result<Unit> {
        return Result.Success(Unit)
    }
}
