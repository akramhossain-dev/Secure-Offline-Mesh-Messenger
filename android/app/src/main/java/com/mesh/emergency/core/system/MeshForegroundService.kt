/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.system

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager as AndroidNotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.mesh.emergency.app.MainActivity
import com.mesh.emergency.core.communication.CommunicationManager
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

/**
 * Ongoing Foreground Service maintaining active BLE scanning, GATT server, and advertising
 * when the app is in the background or the screen is off.
 */
@AndroidEntryPoint
class MeshForegroundService : Service() {

    @Inject
    lateinit var communicationManager: CommunicationManager

    override fun onCreate() {
        super.onCreate()
        Timber.d("SERVICE STARTED — MeshForegroundService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("SERVICE STARTED — MeshForegroundService onStartCommand")
        createNotificationChannel()
        val notification = buildServiceNotification()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                    )
                } catch (se: SecurityException) {
                    // BLE runtime permissions not yet granted — start without type.
                    // The service will be restarted with the correct type once the user
                    // grants Bluetooth permissions in the onboarding flow.
                    Timber.w(se, "SERVICE: BLE permissions not granted yet — starting FGS without connectedDevice type")
                    startForeground(NOTIFICATION_ID, notification)
                }
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Timber.e(e, "SERVICE: startForeground failed — service will retry on next start")
            stopSelf()
            return START_NOT_STICKY
        }

        return START_STICKY

    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Timber.d("SERVICE STOPPED — MeshForegroundService destroyed")
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Mesh Network Service",
                AndroidNotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the emergency mesh communication active in the background"
                setShowBadge(false)
            }
            val manager = getSystemService(AndroidNotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildServiceNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentTitle("Emergency Mesh Active")
            .setContentText("Monitoring mesh network and listening for messages")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "channel_mesh_service"
        private const val NOTIFICATION_ID = 9999
    }
}
