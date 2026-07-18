/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.audio

import com.mesh.emergency.core.common.result.Result
import com.mesh.emergency.data.local.entity.VoiceMessageEntity
import kotlinx.coroutines.flow.Flow

/**
 * Interface contract coordinating voice recording files and forwarding queues.
 */
interface VoiceManager {
    /** Stream list of recorded voice messages. */
    fun getRecordedVoiceMessages(): Flow<Result<List<VoiceMessageEntity>>>

    /** Records a voice message and forwards it via messaging queues. */
    suspend fun recordAndSendVoice(receiverId: String, qualityMode: String): Result<VoiceMessageEntity>

    /** Removes a voice message from local caches. */
    suspend fun deleteVoiceRecord(voiceId: String): Result<Unit>
}
