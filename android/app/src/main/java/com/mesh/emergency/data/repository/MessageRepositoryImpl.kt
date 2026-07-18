/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.repository

import com.mesh.emergency.core.common.result.Result
import com.mesh.emergency.domain.repository.MessageDomainModel
import com.mesh.emergency.domain.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data layer implementation of [MessageRepository] returning stubbed values.
 */
@Singleton
class MessageRepositoryImpl @Inject constructor() : MessageRepository {

    override fun getMessages(contactId: String): Flow<Result<List<MessageDomainModel>>> {
        return flowOf(Result.Success(emptyList()))
    }

    override suspend fun sendMessage(recipientId: String, content: String): Result<Unit> {
        return Result.Success(Unit)
    }

    override suspend fun deleteMessage(messageId: String): Result<Unit> {
        return Result.Success(Unit)
    }
}
