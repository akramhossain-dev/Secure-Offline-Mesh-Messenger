/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.notification

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton notifier allowing transport layers to push real-time message events
 * directly to the foreground Compose UI layer.
 */
@Singleton
class MessageNotifier @Inject constructor() {

    sealed interface Event {
        data class PrivateMessage(
            val messageId: String,
            val senderId: String,
            val senderName: String,
            val text: String
        ) : Event

        data class GlobalMessage(
            val messageId: String,
            val senderId: String,
            val senderName: String,
            val text: String
        ) : Event
    }

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 32)
    val events: SharedFlow<Event> = _events.asSharedFlow()

    fun notifyPrivateMessage(messageId: String, senderId: String, senderName: String, text: String) {
        _events.tryEmit(Event.PrivateMessage(messageId, senderId, senderName, text))
    }

    fun notifyGlobalMessage(messageId: String, senderId: String, senderName: String, text: String) {
        _events.tryEmit(Event.GlobalMessage(messageId, senderId, senderName, text))
    }
}
