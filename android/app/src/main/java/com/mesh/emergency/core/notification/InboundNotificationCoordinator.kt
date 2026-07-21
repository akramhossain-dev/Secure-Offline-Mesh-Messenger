/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.notification

import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.mesh.emergency.app.MeshApplication.AppLifecycleTracker
import com.mesh.emergency.core.domain.AppStateRepository
import com.mesh.emergency.core.overlay.ChatHeadService
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single authoritative coordinator for handling inbound message audio feedback,
 * system bar notifications, and floating Chat Heads.
 */
@Singleton
class InboundNotificationCoordinator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationManager: NotificationManager,
    private val appStateRepository: AppStateRepository
) {

    /**
     * Handles inbound message notification logic cleanly and consistently:
     * - Plays a subtle notification sound if app is in foreground.
     * - Displays system notification and triggers Chat Head if app is in background.
     */
    fun notifyInboundMessage(
        messageId: String,
        senderName: String,
        text: String,
        isGlobal: Boolean,
        sourceId: String = "global"
    ) {
        if (AppLifecycleTracker.isAppInForeground) {
            playReceiveSound()
        } else {
            val alert = AlertModel(
                id = messageId,
                type = AlertType.MESSAGE_ALERT,
                title = senderName,
                description = if (isGlobal) "[Global] $text" else text,
                priority = AlertPriority.NORMAL,
                timestamp = System.currentTimeMillis(),
                source = sourceId,
                status = "ACTIVE"
            )
            notificationManager.showNotification(alert)

            triggerChatHeadIfAllowed(isGlobal, sourceId, senderName)
        }
    }

    private fun triggerChatHeadIfAllowed(isGlobal: Boolean, sourceId: String, senderName: String) {
        try {
            val canDraw = Settings.canDrawOverlays(context)
            val headsOn = appStateRepository.appState.value.chatHeadsEnabled
            val isAppInForeground = com.mesh.emergency.app.MeshApplication.AppLifecycleTracker.isAppInForeground
            if (canDraw && headsOn && !isAppInForeground) {
                val convId = if (isGlobal) "global" else sourceId
                val label  = if (isGlobal) "Global Mesh Chat" else senderName
                val headIntent = Intent(context, ChatHeadService::class.java).apply {
                    action = ChatHeadService.ACTION_SHOW_HEAD
                    putExtra(ChatHeadService.EXTRA_CONV_ID, convId)
                    putExtra(ChatHeadService.EXTRA_LABEL, label)
                }
                ContextCompat.startForegroundService(context, headIntent)
                Timber.d("INBOUND_COORD: Chat Head triggered convId=$convId sender=$senderName")
            } else {
                Timber.d("INBOUND_COORD: Chat Head skipped — canDraw=$canDraw headsOn=$headsOn isAppInForeground=$isAppInForeground")
            }
        } catch (e: Exception) {
            Timber.e(e, "INBOUND_COORD: Failed to start ChatHeadService")
        }
    }

    private fun playReceiveSound() {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context, uri)
            ringtone.play()
        } catch (e: Exception) {
            Timber.e(e, "INBOUND_COORD: Failed to play receive sound")
        }
    }
}
