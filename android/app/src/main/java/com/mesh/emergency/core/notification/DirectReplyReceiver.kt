/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import com.mesh.emergency.core.messaging.MessagingService
import com.mesh.emergency.data.local.LocalDataSource
import com.mesh.emergency.data.local.entity.ConversationEntity
import com.mesh.emergency.data.local.entity.DbDeliveryStatus
import com.mesh.emergency.data.local.entity.DbMessagePriority
import com.mesh.emergency.data.local.entity.DbMessageType
import com.mesh.emergency.data.local.entity.GlobalMessageEntity
import com.mesh.emergency.data.local.entity.MessageEntity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

/**
 * BroadcastReceiver handling system notification inline Direct Reply actions (RemoteInput).
 * Users can reply directly from notification banners without launching the app.
 */
@AndroidEntryPoint
class DirectReplyReceiver : BroadcastReceiver() {

    @Inject
    lateinit var messagingService: MessagingService

    @Inject
    lateinit var localDataSource: LocalDataSource

    @Inject
    lateinit var notificationManager: NotificationManager

    override fun onReceive(context: Context, intent: Intent) {
        val results = RemoteInput.getResultsFromIntent(intent) ?: return
        val replyText = results.getCharSequence(KEY_TEXT_REPLY)?.toString()?.trim()
        if (replyText.isNullOrEmpty()) return

        val convId = intent.getStringExtra(EXTRA_CONV_ID) ?: return
        val label  = intent.getStringExtra(EXTRA_LABEL) ?: "Chat"
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, convId.hashCode())

        Timber.d("DIRECT_REPLY: Received inline reply — convId=$convId text='${replyText.take(40)}'")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val currentUser = localDataSource.getCurrentUser().firstOrNull()
                val senderId   = currentUser?.entityId ?: "self"
                val senderName = currentUser?.nickname?.takeIf { it.isNotBlank() }
                    ?: currentUser?.username?.takeIf { it.isNotBlank() }
                    ?: "Me"
                val msgId = UUID.randomUUID().toString()
                val now   = System.currentTimeMillis()

                if (convId == "global") {
                    val entity = GlobalMessageEntity(
                        messageId      = msgId,
                        senderId       = senderId,
                        senderName     = senderName,
                        content        = replyText,
                        timestamp      = now,
                        createdAt      = now,
                        updatedAt      = now,
                        edited         = false,
                        deleted        = false,
                        deliveryStatus = "SENT",
                        readStatus     = "READ",
                        syncState      = "SYNCED"
                    )
                    localDataSource.insertGlobalMessage(entity)
                    messagingService.sendGlobalMessage(
                        messageId  = msgId,
                        senderId   = senderId,
                        senderName = senderName,
                        text       = replyText
                    )
                } else {
                    val entity = MessageEntity(
                        entityId       = msgId,
                        messageId      = msgId,
                        conversationId = convId,
                        senderId       = senderId,
                        senderName     = senderName,
                        recipientId    = convId,
                        content        = replyText,
                        timestamp      = now,
                        createdAt      = now,
                        updatedAt      = now,
                        edited         = false,
                        deleted        = false,
                        deliveryStatus = DbDeliveryStatus.SENT,
                        readStatus     = "READ",
                        syncState      = "SYNCED",
                        type           = DbMessageType.TEXT,
                        priority       = DbMessagePriority.MEDIUM,
                        expiryTime     = now + 86_400_000L,
                        retryCount     = 0
                    )
                    localDataSource.insertMessage(entity)
                    localDataSource.insertConversation(
                        ConversationEntity(
                            entityId      = convId,
                            title         = label,
                            lastMessageId = msgId,
                            unreadCount   = 0,
                            updatedAt     = now
                        )
                    )
                    val json = org.json.JSONObject().apply {
                        put("type", "chat")
                        put("id",   msgId)
                        put("from", senderId)
                        put("to",   convId)
                        put("text", replyText)
                        put("ts",   now)
                    }
                    messagingService.sendDeliveryReceipt(msgId, senderId, convId)
                }

                // Update notification to acknowledge reply sent
                val alert = AlertModel(
                    id = msgId,
                    type = AlertType.MESSAGE_ALERT,
                    title = "Replied to $label",
                    description = replyText,
                    priority = AlertPriority.LOW,
                    timestamp = now,
                    source = convId,
                    status = "ACTIVE"
                )
                notificationManager.cancelNotification(convId)
                Timber.d("DIRECT_REPLY: Reply sent and DB updated — msgId=$msgId")
            } catch (e: Exception) {
                Timber.e(e, "DIRECT_REPLY: Failed to process inline reply")
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val KEY_TEXT_REPLY = "key_text_reply"
        const val EXTRA_CONV_ID  = "extra_conv_id"
        const val EXTRA_LABEL    = "extra_label"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }
}
