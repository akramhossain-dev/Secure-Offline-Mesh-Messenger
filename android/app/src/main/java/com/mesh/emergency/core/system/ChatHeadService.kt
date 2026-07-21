/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.system

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager as AndroidNotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.mesh.emergency.app.MainActivity
import com.mesh.emergency.core.designsystem.theme.MeshTheme
import com.mesh.emergency.core.domain.AppStateRepository
import com.mesh.emergency.core.messaging.MessagingService
import com.mesh.emergency.core.navigation.NavRoutes
import com.mesh.emergency.data.local.LocalDataSource
import com.mesh.emergency.feature.overlay.OverlayChatScreen
import com.mesh.emergency.feature.overlay.OverlayConversationViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Foreground Service managing true Facebook Messenger-style floating Chat Heads.
 *
 * Uses [WindowManager] with [WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY]
 * to render draggable, edge-snapping circular avatars and expandable chat popups
 * directly over other apps — no Activity needed.
 *
 * Key design decisions:
 * - Each conversation has its **own** [ViewModelStore] so ViewModels are properly scoped.
 * - Hilt ViewModel injection via [ViewModelProvider] + Hilt entry point.
 * - Snap-to-edge uses [ValueAnimator] for smooth spring-feel motion.
 * - Unread badge state is driven by a [MutableState] reactive slot per head.
 * - All WindowManager operations are guarded in try-catch to avoid crashes on
 *   race conditions when the service is destroyed while animating.
 */
@AndroidEntryPoint
class ChatHeadService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    // ── Injected Dependencies ────────────────────────────────────────────────
    @Inject lateinit var messagingService: MessagingService
    @Inject lateinit var localDataSource: LocalDataSource
    @Inject lateinit var communicationManager: com.mesh.emergency.core.communication.CommunicationManager
    @Inject lateinit var appStateRepository: AppStateRepository

    // ── Lifecycle / ViewModel Infrastructure ─────────────────────────────────
    private val lifecycleRegistry            = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    /** Single shared store for the service-level LifecycleOwner. */
    private val serviceViewModelStore = ViewModelStore()

    /** Per-conversation stores: each popup gets its own isolated ViewModel. */
    private val convViewModelStores = mutableMapOf<String, ViewModelStore>()

    override val lifecycle: Lifecycle             get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore   get() = serviceViewModelStore
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    // ── System Services & Scope ───────────────────────────────────────────────
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var windowManager: WindowManager

    /** Active chat head entries keyed by conversation ID. */
    private val chatHeads = mutableMapOf<String, ChatHeadHolder>()

    // ─────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startForegroundNotification()
        observeInboundMessages()
        Timber.d("CHAT_HEAD_SERVICE: onCreate")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let { dispatchIntent(it) }
        return START_STICKY
    }

    override fun onDestroy() {
        clearAllChatHeads()
        scope.cancel()
        // Clear all per-conversation stores to avoid ViewModel leaks
        convViewModelStores.values.forEach { it.clear() }
        convViewModelStores.clear()
        serviceViewModelStore.clear()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        Timber.d("CHAT_HEAD_SERVICE: onDestroy")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ─────────────────────────────────────────────────────────────────────────
    // Intent Dispatch
    // ─────────────────────────────────────────────────────────────────────────

    private fun dispatchIntent(intent: Intent) {
        val convId = intent.getStringExtra(EXTRA_CONV_ID) ?: "global"
        val label  = intent.getStringExtra(EXTRA_LABEL)  ?: "Chat"
        when (intent.action) {
            ACTION_SHOW_HEAD   -> showChatHead(convId, label)
            ACTION_REMOVE_HEAD -> removeChatHead(convId)
            ACTION_CLEAR_ALL   -> clearAllChatHeads()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Inbound Message Observer → triggers chat head creation
    // ─────────────────────────────────────────────────────────────────────────

    private fun observeInboundMessages() {
        scope.launch {
            messagingService.observeIncomingMessages().collect { inbound ->
                if (!Settings.canDrawOverlays(this@ChatHeadService)) return@collect
                val appState = appStateRepository.appState.value
                if (!appState.chatHeadsEnabled) return@collect

                when (inbound) {
                    is com.mesh.emergency.core.messaging.IncomingMessage.GlobalChat -> {
                        val currentUser = localDataSource.getCurrentUser().firstOrNull()
                        if (inbound.senderId != currentUser?.entityId) {
                            val existing = chatHeads["global"]
                            if (existing != null) {
                                // Head already visible — just bump the badge
                                existing.unreadCount.intValue += 1
                                existing.pulseEnabled.value = true
                            } else {
                                showChatHead("global", "Global: ${inbound.senderName}")
                            }
                        }
                    }
                    is com.mesh.emergency.core.messaging.IncomingMessage.PrivateChat -> {
                        val currentUser = localDataSource.getCurrentUser().firstOrNull()
                        if (inbound.senderId != currentUser?.entityId) {
                            val senderUser = localDataSource.getUserById(inbound.senderId)
                            val name = senderUser?.nickname?.takeIf { it.isNotBlank() }
                                ?: senderUser?.username?.takeIf { it.isNotBlank() }
                                ?: "Contact-${inbound.senderId.take(6)}"
                            val existing = chatHeads[inbound.senderId]
                            if (existing != null) {
                                existing.unreadCount.intValue += 1
                                existing.pulseEnabled.value = true
                            } else {
                                showChatHead(inbound.senderId, name)
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Chat Head — Show / Remove
    // ─────────────────────────────────────────────────────────────────────────

    @SuppressLint("ClickableViewAccessibility")
    fun showChatHead(convId: String, label: String) {
        if (!Settings.canDrawOverlays(this)) {
            Timber.w("CHAT_HEAD_SERVICE: SYSTEM_ALERT_WINDOW not granted — skipping")
            return
        }
        if (chatHeads.containsKey(convId)) {
            Timber.d("CHAT_HEAD_SERVICE: Head already exists for convId=$convId")
            return
        }

        val unreadCount  = mutableIntStateOf(1)
        val pulseEnabled = mutableStateOf(true)

        val headParams = overlayLayoutParams(
            width  = WindowManager.LayoutParams.WRAP_CONTENT,
            height = WindowManager.LayoutParams.WRAP_CONTENT,
            flags  = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                     WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 30
            y = 300 + chatHeads.size * 70   // stack multiple heads vertically
        }

        val headView = ComposeView(this).also { cv ->
            cv.setViewTreeLifecycleOwner(this)
            cv.setViewTreeViewModelStoreOwner(this)
            cv.setViewTreeSavedStateRegistryOwner(this)
            cv.setContent {
                MeshTheme {
                    ChatHeadAvatarCompose(
                        label        = label,
                        unreadCount  = unreadCount.intValue,
                        pulseEnabled = pulseEnabled.value
                    )
                }
            }
        }

        val holder = ChatHeadHolder(
            convId       = convId,
            label        = label,
            headView     = headView,
            headParams   = headParams,
            unreadCount  = unreadCount,
            pulseEnabled = pulseEnabled
        )
        chatHeads[convId] = holder

        // ── Trash Zone — shown while dragging, dismissed on drop ──────────
        val screenH   = resources.displayMetrics.heightPixels
        val screenW   = resources.displayMetrics.widthPixels
        val trashZoneH = (screenH * 0.15f).toInt()
        val trashParams = overlayLayoutParams(
            width  = screenW,
            height = trashZoneH,
            flags  = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                     WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                     WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.START
            x = 0; y = 0
        }
        val trashView = ComposeView(this).also { cv ->
            cv.setViewTreeLifecycleOwner(this)
            cv.setViewTreeViewModelStoreOwner(this)
            cv.setViewTreeSavedStateRegistryOwner(this)
            cv.setContent {
                MeshTheme {
                    TrashDropZone()
                }
            }
        }
        var trashVisible = false

        // Drag + tap touch handler
        var startX = 0; var startY = 0
        var touchX = 0f; var touchY = 0f

        headView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = headParams.x; startY = headParams.y
                    touchX = event.rawX;  touchY = event.rawY
                    pulseEnabled.value = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    headParams.x = startX + (event.rawX - touchX).toInt()
                    headParams.y = startY + (event.rawY - touchY).toInt()
                    safeUpdateLayout(headView, headParams)
                    // Show trash zone once user drags more than 20px
                    val moved = kotlin.math.abs(event.rawX - touchX) > 20f ||
                                kotlin.math.abs(event.rawY - touchY) > 20f
                    if (moved && !trashVisible) {
                        try { windowManager.addView(trashView, trashParams); trashVisible = true }
                        catch (_: Exception) {}
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    // Remove trash zone
                    if (trashVisible) {
                        safeRemoveView(trashView); trashVisible = false
                    }
                    val dx = kotlin.math.abs(event.rawX - touchX)
                    val dy = kotlin.math.abs(event.rawY - touchY)
                    val droppedInTrash = event.rawY > screenH - trashZoneH
                    when {
                        dx < 12f && dy < 12f -> togglePopup(holder)   // Tap
                        droppedInTrash       -> removeChatHead(convId) // Drop to trash
                        else                 -> snapToEdge(headView, headParams) // Normal drag
                    }
                    true
                }
                else -> false
            }
        }

        try {
            windowManager.addView(headView, headParams)
            Timber.d("CHAT_HEAD_SERVICE: Head added — convId=$convId")
        } catch (e: Exception) {
            Timber.e(e, "CHAT_HEAD_SERVICE: Failed to add head view")
            chatHeads.remove(convId)
        }
    }

    fun removeChatHead(convId: String) {
        chatHeads.remove(convId)?.let { holder ->
            closePopup(holder)
            safeRemoveView(holder.headView)
            // Clean up the per-conversation ViewModel store
            convViewModelStores.remove(convId)?.clear()
            Timber.d("CHAT_HEAD_SERVICE: Head removed — convId=$convId")
        }
    }

    fun clearAllChatHeads() {
        chatHeads.keys.toList().forEach { removeChatHead(it) }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Popup Conversation — Show / Toggle / Close
    // ─────────────────────────────────────────────────────────────────────────

    private fun togglePopup(holder: ChatHeadHolder) {
        if (holder.popupView != null) closePopup(holder)
        else openPopup(holder)
    }

    private fun openPopup(holder: ChatHeadHolder) {
        val dm     = resources.displayMetrics
        val popupW = (dm.widthPixels  * 0.94f).toInt()
        val popupH = (dm.heightPixels * 0.87f).toInt()

        val popupParams = overlayLayoutParams(
            width  = popupW,
            height = popupH,
            flags  = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                     WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        ).apply {
            gravity   = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y         = 12
            // ── CRITICAL: without this the soft keyboard won't show ──
            softInputMode = android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
                            android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
        }

        // Get or create the per-conversation ViewModelStore
        val convStore = convViewModelStores.getOrPut(holder.convId) { ViewModelStore() }
        val convStoreOwner = object : ViewModelStoreOwner {
            override val viewModelStore: ViewModelStore = convStore
        }

        // Build a factory that supplies the service's injected deps to the ViewModel
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                OverlayConversationViewModel(localDataSource, messagingService, communicationManager) as T
        }

        // Obtain ViewModel via per-conv store — prevents cross-conversation state bleed
        val viewModel = ViewModelProvider(
            convStoreOwner,
            factory
        )[OverlayConversationViewModel::class.java]
        viewModel.init(holder.convId, holder.label)

        // Reset unread count when popup opens
        holder.unreadCount.intValue = 0
        holder.pulseEnabled.value   = false

        val popupView = ComposeView(this).also { cv ->
            cv.setViewTreeLifecycleOwner(this)
            cv.setViewTreeViewModelStoreOwner(this)
            cv.setViewTreeSavedStateRegistryOwner(this)
            cv.setContent {
                MeshTheme {
                    OverlayChatScreen(
                        viewModel = viewModel,
                        onClose   = { closePopup(holder) },
                        onExpand  = { expandToFullApp(holder.convId, holder.label) }
                    )
                }
            }
        }

        holder.popupView   = popupView
        holder.popupParams = popupParams

        try {
            windowManager.addView(popupView, popupParams)
            Timber.d("CHAT_HEAD_SERVICE: Popup opened — convId=${holder.convId}")
        } catch (e: Exception) {
            Timber.e(e, "CHAT_HEAD_SERVICE: Failed to add popup view")
            holder.popupView   = null
            holder.popupParams = null
        }
    }

    private fun closePopup(holder: ChatHeadHolder) {
        holder.popupView?.let { safeRemoveView(it) }
        holder.popupView   = null
        holder.popupParams = null
        Timber.d("CHAT_HEAD_SERVICE: Popup closed — convId=${holder.convId}")
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Expand to full MainActivity chat screen
    // ─────────────────────────────────────────────────────────────────────────

    private fun expandToFullApp(convId: String, label: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (convId == "global") {
                putExtra("navRoute", NavRoutes.GLOBAL_CHAT)
            } else {
                putExtra("convId", convId)
                putExtra("label",  label)
            }
        }
        startActivity(intent)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Snap to Edge — smooth ValueAnimator
    // ─────────────────────────────────────────────────────────────────────────

    private fun snapToEdge(view: View, params: WindowManager.LayoutParams) {
        val screenW   = resources.displayMetrics.widthPixels
        val targetX   = if (params.x < screenW / 2) 16 else screenW - view.width - 16
        val startX    = params.x

        ValueAnimator.ofInt(startX, targetX).apply {
            duration    = 280L
            interpolator = DecelerateInterpolator(1.8f)
            addUpdateListener { anim ->
                params.x = anim.animatedValue as Int
                safeUpdateLayout(view, params)
            }
        }.start()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // WindowManager helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun overlayLayoutParams(width: Int, height: Int, flags: Int): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

        return WindowManager.LayoutParams(width, height, type, flags, PixelFormat.TRANSLUCENT)
    }

    private fun safeUpdateLayout(view: View, params: WindowManager.LayoutParams) {
        try { windowManager.updateViewLayout(view, params) }
        catch (e: Exception) { /* view already removed */ }
    }

    private fun safeRemoveView(view: View) {
        try { windowManager.removeView(view) }
        catch (e: Exception) { /* already removed */ }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Foreground Service Notification (keeps service alive in background)
    // ─────────────────────────────────────────────────────────────────────────

    private fun startForegroundNotification() {
        val channelId = "channel_chat_heads"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Chat Heads",
                AndroidNotificationManager.IMPORTANCE_MIN
            ).apply { setShowBadge(false) }
            val mgr = getSystemService(AndroidNotificationManager::class.java)
            mgr?.createNotificationChannel(channel)
        }

        val tapIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle("Emergency Connect")
            .setContentText("Chat heads active")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentIntent(tapIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()

        startForeground(FOREGROUND_ID, notification)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Data Holder
    // ─────────────────────────────────────────────────────────────────────────

    private data class ChatHeadHolder(
        val convId: String,
        val label: String,
        val headView: View,
        val headParams: WindowManager.LayoutParams,
        val unreadCount: androidx.compose.runtime.MutableIntState,
        val pulseEnabled: MutableState<Boolean>,
        var popupView: View? = null,
        var popupParams: WindowManager.LayoutParams? = null
    )

    // ─────────────────────────────────────────────────────────────────────────
    // Companion
    // ─────────────────────────────────────────────────────────────────────────

    companion object {
        const val ACTION_SHOW_HEAD   = "com.mesh.emergency.SHOW_CHAT_HEAD"
        const val ACTION_REMOVE_HEAD = "com.mesh.emergency.REMOVE_CHAT_HEAD"
        const val ACTION_CLEAR_ALL   = "com.mesh.emergency.CLEAR_ALL_CHAT_HEADS"
        const val EXTRA_CONV_ID      = "extra_conv_id"
        const val EXTRA_LABEL        = "extra_label"
        private const val FOREGROUND_ID = 8888
    }
}
