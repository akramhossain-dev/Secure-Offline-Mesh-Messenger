/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager as AndroidNotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import com.mesh.emergency.app.MainActivity
import com.mesh.emergency.core.messaging.MessagingService
import com.mesh.emergency.data.local.LocalDataSource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import timber.log.Timber
import javax.inject.Inject

/**
 * Clean, production-quality Foreground Service orchestrating floating Chat Heads,
 * using [OverlayPermission], [OverlayManager], [ChatHeadManager], and [PopupConversationController].
 */
@AndroidEntryPoint
class ChatHeadService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    companion object {
        const val ACTION_SHOW_HEAD   = "com.mesh.emergency.ACTION_SHOW_HEAD"
        const val ACTION_REMOVE_HEAD = "com.mesh.emergency.ACTION_REMOVE_HEAD"
        const val ACTION_CLEAR_ALL   = "com.mesh.emergency.ACTION_CLEAR_ALL"

        const val EXTRA_CONV_ID = "extra_conv_id"
        const val EXTRA_LABEL   = "extra_label"

        fun removeHead(context: Context, convId: String) {
            try {
                val intent = Intent(context, ChatHeadService::class.java).apply {
                    action = ACTION_REMOVE_HEAD
                    putExtra(EXTRA_CONV_ID, convId)
                }
                context.startService(intent)
            } catch (e: Exception) {
                Timber.w(e, "CHAT_HEAD_SERVICE: Failed to send removeHead intent")
            }
        }

        private const val NOTIF_CHANNEL_ID = "chat_head_service_channel"
        private const val NOTIF_ID         = 9090
    }

    // ── Injected Dependencies ────────────────────────────────────────────────
    @Inject lateinit var messagingService: MessagingService
    @Inject lateinit var localDataSource: LocalDataSource
    @Inject lateinit var overlayPermission: OverlayPermission
    @Inject lateinit var overlayManager: OverlayManager
    @Inject lateinit var chatHeadManager: ChatHeadManager
    @Inject lateinit var popupController: PopupConversationController

    // ── Lifecycle & ViewModel Scoping ───────────────────────────────────────
    private val lifecycleRegistry            = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    private val serviceViewModelStore = ViewModelStore()
    private val convViewModelStores   = mutableMapOf<String, ViewModelStore>()

    override val lifecycle: Lifecycle             get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore   get() = serviceViewModelStore
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var windowManager: WindowManager

    /** Active chat head holders keyed by conversation ID. */
    private val chatHeads = mutableMapOf<String, ChatHeadHolder>()

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        windowManager = overlayManager.getWindowManager(this)
        startForegroundNotification()
        restoreActiveHeadsState()
        Timber.d("CHAT_HEAD_SERVICE: onCreate initialized")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundNotification()
        intent?.let { dispatchIntent(it) }
        return START_STICKY
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        val screenW = resources.displayMetrics.widthPixels
        val screenH = resources.displayMetrics.heightPixels
        chatHeads.values.forEach { holder ->
            holder.headParams.x = holder.headParams.x.coerceIn(16, screenW - 140)
            holder.headParams.y = holder.headParams.y.coerceIn(50, screenH - 200)
            overlayManager.safeUpdateViewLayout(windowManager, holder.headView, holder.headParams)

            holder.popupView?.let { pView ->
                holder.popupParams?.let { pParams ->
                    pParams.width  = (screenW * 0.94f).toInt()
                    pParams.height = (screenH * 0.87f).toInt()
                    overlayManager.safeUpdateViewLayout(windowManager, pView, pParams)
                }
            }
        }
    }

    override fun onDestroy() {
        clearAllChatHeads()
        scope.cancel()
        convViewModelStores.values.forEach { it.clear() }
        convViewModelStores.clear()
        serviceViewModelStore.clear()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        Timber.d("CHAT_HEAD_SERVICE: onDestroy clean stop")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun dispatchIntent(intent: Intent) {
        val convId = intent.getStringExtra(EXTRA_CONV_ID) ?: "global"
        val label  = intent.getStringExtra(EXTRA_LABEL)  ?: "Chat"
        when (intent.action) {
            ACTION_SHOW_HEAD   -> showChatHead(convId, label)
            ACTION_REMOVE_HEAD -> removeChatHead(convId)
            ACTION_CLEAR_ALL   -> clearAllChatHeads()
        }
    }

    private fun showChatHead(convId: String, label: String) {
        if (!overlayPermission.canDrawOverlays(this)) {
            Timber.w("CHAT_HEAD_SERVICE: Skipping head creation — overlay permission denied")
            return
        }

        val existing = chatHeads[convId]
        if (existing != null) {
            existing.label = label
            existing.unreadState.value++
            existing.pulseState.value = true
            Timber.d("CHAT_HEAD_SERVICE: Updated existing head convId=$convId unread=${existing.unreadState.value}")
        } else {
            val holder = chatHeadManager.createChatHead(
                context = this,
                service = this,
                windowManager = windowManager,
                convId = convId,
                label = label,
                stackIndex = chatHeads.size,
                onTap = { h ->
                    popupController.togglePopup(
                        context = this,
                        service = this,
                        windowManager = windowManager,
                        holder = h,
                        convViewModelStores = convViewModelStores,
                        onCloseRequested = { saveActiveHeadsState() }
                    )
                },
                onRemove = { h ->
                    removeChatHead(h.convId)
                }
            )
            chatHeads[convId] = holder
            saveActiveHeadsState()
            Timber.d("CHAT_HEAD_SERVICE: Spawned new head convId=$convId label='$label'")
        }
    }

    private fun removeChatHead(convId: String) {
        val holder = chatHeads.remove(convId) ?: return
        popupController.closePopup(this, windowManager, holder)
        overlayManager.safeRemoveView(windowManager, holder.headView)

        convViewModelStores.remove(convId)?.clear()
        saveActiveHeadsState()

        if (chatHeads.isEmpty()) {
            stopSelf()
        }
    }

    private fun clearAllChatHeads() {
        val keys = chatHeads.keys.toList()
        keys.forEach { removeChatHead(it) }
        stopSelf()
    }

    fun setFloatingHeadsVisible(visible: Boolean) {
        val visibility = if (visible) android.view.View.VISIBLE else android.view.View.GONE
        chatHeads.values.forEach { it.headView.visibility = visibility }
    }

    fun getActiveHeads(): List<ChatHeadHolder> = chatHeads.values.toList()

    fun switchActivePopup(targetConvId: String) {
        val holder = chatHeads[targetConvId] ?: return
        // Close current popup
        chatHeads.values.forEach { h -> popupController.closePopup(this, windowManager, h) }
        // Open target popup
        popupController.openPopup(
            context = this,
            service = this,
            windowManager = windowManager,
            holder = holder,
            convViewModelStores = convViewModelStores,
            onCloseRequested = { saveActiveHeadsState() }
        )
    }

    private fun saveActiveHeadsState() {
        try {
            val prefs = getSharedPreferences("chat_heads_state", Context.MODE_PRIVATE)
            val entries = chatHeads.map { (convId, holder) -> "$convId|${holder.label}" }.toSet()
            prefs.edit().putStringSet("active_heads", entries).apply()
        } catch (e: Exception) {
            Timber.w(e, "CHAT_HEAD_SERVICE: Failed to save active heads state")
        }
    }

    private fun restoreActiveHeadsState() {
        try {
            if (com.mesh.emergency.app.MeshApplication.AppLifecycleTracker.isAppInForeground) return
            val prefs = getSharedPreferences("chat_heads_state", Context.MODE_PRIVATE)
            val entries = prefs.getStringSet("active_heads", emptySet()) ?: emptySet()
            for (entry in entries) {
                val parts = entry.split("|")
                if (parts.size >= 2) {
                    showChatHead(parts[0], parts[1])
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "CHAT_HEAD_SERVICE: Failed to restore active heads state")
        }
    }

    private fun startForegroundNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIF_CHANNEL_ID,
                "Floating Chat Heads",
                AndroidNotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Keeps floating chat heads active in background"
                setShowBadge(false)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as AndroidNotificationManager
            nm.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, NOTIF_CHANNEL_ID)
            .setContentTitle("Floating Chat Active")
            .setContentText("Emergency Mesh Chat Head is active")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                startForeground(NOTIF_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } catch (e: Exception) {
                startForeground(NOTIF_ID, notification)
            }
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }
}
