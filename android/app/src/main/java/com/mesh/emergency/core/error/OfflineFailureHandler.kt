/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.error

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles offline-specific failure scenarios and emits user-visible error events.
 *
 * Provides a centralized point for:
 * - Queue overflow failures
 * - Storage-full conditions
 * - Decryption failures
 * - Recovery state suggestions
 */
@Singleton
class OfflineFailureHandler @Inject constructor() {

    private val _failureEvents = MutableSharedFlow<OfflineFailureEvent>(extraBufferCapacity = 16)
    val failureEvents: SharedFlow<OfflineFailureEvent> = _failureEvents.asSharedFlow()

    /** Called when the sync queue exceeds capacity. */
    fun onQueueOverflow(queueSize: Int) {
        val event = OfflineFailureEvent.QueueOverflow(queueSize)
        _failureEvents.tryEmit(event)
        Timber.w("OfflineFailure: Queue overflow — $queueSize pending operations")
    }

    /** Called when device storage is critically low. */
    fun onStorageLow(availableBytes: Long) {
        val event = OfflineFailureEvent.StorageLow(availableBytes)
        _failureEvents.tryEmit(event)
        Timber.w("OfflineFailure: Storage low — ${availableBytes / 1024} KB remaining")
    }

    /** Called when a message decryption fails. */
    fun onDecryptionFailure(messageId: String) {
        val event = OfflineFailureEvent.DecryptionFailed(messageId)
        _failureEvents.tryEmit(event)
        Timber.e("OfflineFailure: Decryption failed for message $messageId")
    }

    /** Called when a database operation fails. */
    fun onDatabaseFailure(operation: String, cause: Throwable) {
        val event = OfflineFailureEvent.DatabaseError(operation, cause.message ?: "Unknown")
        _failureEvents.tryEmit(event)
        Timber.e(cause, "OfflineFailure: DB operation '$operation' failed")
    }

    /** Called when recovery is possible — provides actionable suggestion. */
    fun onRecoveryAvailable(suggestion: String) {
        _failureEvents.tryEmit(OfflineFailureEvent.RecoverySuggestion(suggestion))
    }
}

/**
 * Sealed hierarchy of offline failure events.
 */
sealed class OfflineFailureEvent {
    data class QueueOverflow(val queueSize: Int) : OfflineFailureEvent()
    data class StorageLow(val availableBytes: Long) : OfflineFailureEvent()
    data class DecryptionFailed(val messageId: String) : OfflineFailureEvent()
    data class DatabaseError(val operation: String, val reason: String) : OfflineFailureEvent()
    data class RecoverySuggestion(val message: String) : OfflineFailureEvent()
}
