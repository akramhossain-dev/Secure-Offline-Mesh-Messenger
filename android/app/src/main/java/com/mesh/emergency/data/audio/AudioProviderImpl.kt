/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import com.mesh.emergency.core.audio.AudioInfo
import com.mesh.emergency.core.audio.AudioProvider
import com.mesh.emergency.core.common.result.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real implementation of [AudioProvider] using Android [MediaRecorder] to capture
 * voice messages and save them as AAC/M4A files in app-private internal storage.
 *
 * Replaces [AudioProviderStub] in production DI binding.
 * Files are saved to: [Context.filesDir]/audio/<timestamp>.m4a
 */
@Singleton
class AudioProviderImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AudioProvider {

    private var recorder: MediaRecorder? = null
    private var activeFilePath: String? = null
    private var recordingStartMs: Long = 0L

    private val audioDir: File
        get() = File(context.filesDir, "audio").also { it.mkdirs() }

    @Suppress("DEPRECATION")
    override fun startRecording(): Result<Unit> {
        if (recorder != null) {
            return Result.Error(IllegalStateException("Recording already in progress"))
        }
        return try {
            val outputFile = File(audioDir, "${System.currentTimeMillis()}.m4a")
            activeFilePath = outputFile.absolutePath

            val newRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                MediaRecorder()
            }

            newRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(128_000)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }

            recorder = newRecorder
            recordingStartMs = System.currentTimeMillis()
            Timber.d("AudioProvider: Recording started → ${outputFile.name}")
            Result.Success(Unit)
        } catch (e: IOException) {
            Timber.e(e, "AudioProvider: Prepare failed")
            cleanupRecorder()
            Result.Error(e)
        } catch (e: Exception) {
            Timber.e(e, "AudioProvider: Start recording failed")
            cleanupRecorder()
            Result.Error(e)
        }
    }

    override fun stopRecording(): Result<AudioInfo> {
        val rec = recorder ?: return Result.Error(IllegalStateException("No active recording"))
        val filePath = activeFilePath ?: return Result.Error(IllegalStateException("No active file path"))
        return try {
            rec.stop()
            val durationMs = System.currentTimeMillis() - recordingStartMs
            val file = File(filePath)
            val fileSize = if (file.exists()) file.length() else 0L
            cleanupRecorder()
            Timber.d("AudioProvider: Recording stopped — ${file.name} (${durationMs}ms, ${fileSize}B)")
            Result.Success(
                AudioInfo(
                    filePath = filePath,
                    durationMs = durationMs,
                    fileSize = fileSize,
                    format = "m4a"
                )
            )
        } catch (e: Exception) {
            Timber.e(e, "AudioProvider: Stop recording failed")
            cleanupRecorder()
            Result.Error(e)
        }
    }

    override fun saveAudio(fileBytes: ByteArray, filename: String): Result<String> {
        return try {
            val dest = File(audioDir, filename)
            dest.writeBytes(fileBytes)
            Timber.d("AudioProvider: Saved audio file → ${dest.name}")
            Result.Success(dest.absolutePath)
        } catch (e: Exception) {
            Timber.e(e, "AudioProvider: Save audio failed")
            Result.Error(e)
        }
    }

    override fun deleteAudio(filePath: String): Result<Unit> {
        return try {
            val file = File(filePath)
            if (file.exists()) {
                file.delete()
                Timber.d("AudioProvider: Deleted ${file.name}")
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "AudioProvider: Delete audio failed")
            Result.Error(e)
        }
    }

    private fun cleanupRecorder() {
        try {
            recorder?.reset()
            recorder?.release()
        } catch (_: Exception) { /* ignore during cleanup */ }
        recorder = null
        activeFilePath = null
        recordingStartMs = 0L
    }
}
