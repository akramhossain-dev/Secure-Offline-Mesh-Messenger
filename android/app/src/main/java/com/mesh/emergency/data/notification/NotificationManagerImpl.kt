/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager as AndroidNotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.mesh.emergency.app.MainActivity
import com.mesh.emergency.core.common.result.Result
import com.mesh.emergency.core.notification.AlertModel
import com.mesh.emergency.core.notification.AlertPriority
import com.mesh.emergency.core.notification.NotificationChannelType
import com.mesh.emergency.core.notification.NotificationManager
import com.mesh.emergency.core.system.NotificationServiceWrapper
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [NotificationManager] coordinating real system notification alerts.
 */
@Singleton
class NotificationManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationServiceWrapper: NotificationServiceWrapper
) : NotificationManager {

    private val preferences = mutableMapOf<NotificationChannelType, ChannelPref>()

    init {
        createNotificationChannels()
    }

    override fun showNotification(alert: AlertModel): Result<Unit> {
        if (!notificationServiceWrapper.areNotificationsEnabled()) {
            return Result.Error(Exception("Notifications disabled in OS settings"))
        }

        val channel = when (alert.priority) {
            AlertPriority.CRITICAL -> NotificationChannelType.EMERGENCY
            AlertPriority.HIGH     -> NotificationChannelType.POWER
            AlertPriority.NORMAL   -> NotificationChannelType.MESSAGE
            AlertPriority.LOW      -> NotificationChannelType.SYSTEM
        }

        val pref = preferences[channel] ?: ChannelPref(true, true, true)
        if (!pref.enabled) {
            return Result.Success(Unit) // Silenced
        }

        val channelId = when (channel) {
            NotificationChannelType.EMERGENCY -> "channel_emergency"
            NotificationChannelType.MESSAGE   -> "channel_message"
            else                              -> "channel_system"
        }

        // Tap action: opens MainActivity and passes conversation details
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("convId", alert.source) // source contains peerId/userId or "global"
            putExtra("label", alert.title)   // title contains peerName or SenderName
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            alert.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(alert.title)
            .setContentText(alert.description)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(
                when (alert.priority) {
                    AlertPriority.CRITICAL -> NotificationCompat.PRIORITY_MAX
                    AlertPriority.HIGH     -> NotificationCompat.PRIORITY_HIGH
                    AlertPriority.NORMAL   -> NotificationCompat.PRIORITY_HIGH
                    AlertPriority.LOW      -> NotificationCompat.PRIORITY_LOW
                }
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        // Set default sound, vibration and lights according to preferences
        var defaults = 0
        if (pref.sound) {
            defaults = defaults or NotificationCompat.DEFAULT_SOUND
        }
        if (pref.vibration) {
            defaults = defaults or NotificationCompat.DEFAULT_VIBRATE
        }
        builder.setDefaults(defaults or NotificationCompat.DEFAULT_LIGHTS)

        Timber.d("NOTIFICATION CREATED — id=${alert.id} title='${alert.title}' text='${alert.description}'")

        val notificationManager = NotificationManagerCompat.from(context)
        try {
            // Check permission (required on Android 13+)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                notificationManager.notify(alert.id.hashCode(), builder.build())
                Timber.d("NOTIFICATION DISPLAYED — id=${alert.id} channel=$channelId")
            } else {
                Timber.w("NOTIFICATION DISPLAYED FAILED — POST_NOTIFICATIONS permission not granted")
            }
        } catch (e: SecurityException) {
            Timber.e(e, "NOTIFICATION DISPLAYED FAILED — SecurityException posting notification")
            return Result.Error(e)
        }

        return Result.Success(Unit)
    }

    override fun cancelNotification(alertId: String): Result<Unit> {
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(alertId.hashCode())
        return Result.Success(Unit)
    }

    override fun createNotificationChannels(): Result<Unit> {
        // Pop preferences
        NotificationChannelType.values().forEach {
            preferences[it] = ChannelPref(true, true, true)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as AndroidNotificationManager
            
            // EMERGENCY Channel
            val emergencyChannel = NotificationChannel(
                "channel_emergency",
                "Emergency Alerts",
                AndroidNotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "SOS and Critical network alarms"
                enableLights(true)
                enableVibration(true)
            }
            manager.createNotificationChannel(emergencyChannel)

            // MESSAGE Channel
            val messageChannel = NotificationChannel(
                "channel_message",
                "Messages",
                AndroidNotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Incoming chat communications"
                enableLights(true)
                enableVibration(true)
            }
            manager.createNotificationChannel(messageChannel)

            // SYSTEM Channel
            val systemChannel = NotificationChannel(
                "channel_system",
                "System Alerts",
                AndroidNotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background synchronization and discovery diagnostics"
            }
            manager.createNotificationChannel(systemChannel)
        }

        return Result.Success(Unit)
    }

    override fun updatePreferences(
        channel: NotificationChannelType,
        enabled: Boolean,
        sound: Boolean,
        vibration: Boolean
    ): Result<Unit> {
        preferences[channel] = ChannelPref(enabled, sound, vibration)
        return Result.Success(Unit)
    }

    private data class ChannelPref(
        val enabled: Boolean,
        val sound: Boolean,
        val vibration: Boolean
    )
}
