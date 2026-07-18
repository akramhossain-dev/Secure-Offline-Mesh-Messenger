/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.audio

import com.mesh.emergency.core.common.result.Result

/**
 * Interface contract representing hardware microphone recording systems.
 */
interface AudioProvider {
    /** Starts recording microphone streams. */
    fun startRecording(): Result<Unit>

    /** Stops recording microphone streams and returns audio metadata. */
    fun stopRecording(): Result<AudioInfo>

    /** Saves audio files to local storage path. */
    fun saveAudio(fileBytes: ByteArray, filename: String): Result<String>

    /** Deletes cached audio files. */
    fun deleteAudio(filePath: String): Result<Unit>
}

/**
 * Metadata info representing recorded audio files.
 */
data class AudioInfo(
    val filePath: String,
    val durationMs: Long,
    val fileSize: Long,
    val format: String
)
