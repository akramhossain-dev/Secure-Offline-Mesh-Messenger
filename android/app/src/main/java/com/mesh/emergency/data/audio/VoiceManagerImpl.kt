/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.audio

import com.mesh.emergency.core.audio.AudioProvider
import com.mesh.emergency.core.audio.VoiceManager
import com.mesh.emergency.core.common.result.Result
import com.mesh.emergency.core.communication.CommunicationManager
import com.mesh.emergency.data.local.LocalDataSource
import com.mesh.emergency.data.local.entity.VoiceMessageEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [VoiceManager] managing audio recordings and forward queues.
 */
@Singleton
class VoiceManagerImpl @Inject constructor(
    private val localDataSource: LocalDataSource,
    private val audioProvider: AudioProvider,
    private val communicationManager: CommunicationManager
) : VoiceManager {

    override fun getRecordedVoiceMessages(): Flow<Result<List<VoiceMessageEntity>>> {
        return localDataSource.getVoiceMessages().map { entities ->
            Result.Success(entities)
        }
    }

    override suspend fun recordAndSendVoice(receiverId: String, qualityMode: String): Result<VoiceMessageEntity> {
        return try {
            audioProvider.startRecording()
            // Simulates some recording length before stopping
            val stopResult = audioProvider.stopRecording()
            if (stopResult is Result.Success) {
                val info = stopResult.data
                val now = System.currentTimeMillis()
                val voice = VoiceMessageEntity(
                    entityId = UUID.randomUUID().toString(),
                    senderId = "local_user_id",
                    receiverId = receiverId,
                    fileReference = info.filePath,
                    duration = info.durationMs,
                    fileSize = info.fileSize,
                    format = info.format,
                    quality = qualityMode,
                    timestamp = now,
                    status = "QUEUED"
                )
                localDataSource.insertVoiceMessage(voice)

                val payload = "[VOICE] Ref:${info.filePath} Size:${info.fileSize}".toByteArray()
                communicationManager.sendMessage(payload)

                Result.Success(voice)
            } else {
                Result.Error(Exception("Audio stop failed"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun deleteVoiceRecord(voiceId: String): Result<Unit> {
        return try {
            localDataSource.deleteVoiceMessage(voiceId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
