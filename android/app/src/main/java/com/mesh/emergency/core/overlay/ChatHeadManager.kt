/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.overlay

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.mesh.emergency.core.designsystem.theme.MeshTheme
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages floating Chat Head bubble instances, drag listeners, edge snapping,
 * unread badges, and trash drop zone removal.
 */
@Singleton
class ChatHeadManager @Inject constructor(
    private val overlayManager: OverlayManager
) {

    /**
     * Creates a draggable circular avatar view for a conversation.
     */
    @SuppressLint("ClickableViewAccessibility")
    fun createChatHead(
        context: Context,
        service: ChatHeadService,
        windowManager: WindowManager,
        convId: String,
        label: String,
        stackIndex: Int,
        onTap: (ChatHeadHolder) -> Unit,
        onRemove: (ChatHeadHolder) -> Unit
    ): ChatHeadHolder {
        val screenW = context.resources.displayMetrics.widthPixels
        val screenH = context.resources.displayMetrics.heightPixels

        val startX = screenW - 140
        val startY = (screenH * 0.25f).toInt()

        val headParams = overlayManager.createHeadLayoutParams(startX, startY)

        val unreadState = mutableIntStateOf(1)
        val pulseState  = mutableStateOf(true)

        val composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(service)
            setViewTreeViewModelStoreOwner(service)
            setViewTreeSavedStateRegistryOwner(service)
            setContent {
                MeshTheme {
                    ChatHeadAvatar(
                        convId      = convId,
                        label       = label,
                        unreadCount = unreadState.value,
                        isPulsing   = pulseState.value
                    )
                }
            }
        }

        val holder = ChatHeadHolder(
            convId      = convId,
            label       = label,
            headView    = composeView,
            headParams  = headParams,
            unreadState = unreadState,
            pulseState  = pulseState
        )

        // Attach touch drag listener with trash drop zone & spring snap logic
        attachDragTouchListener(
            context = context,
            service = service,
            windowManager = windowManager,
            holder = holder,
            onTap = { onTap(holder) },
            onRemove = { onRemove(holder) }
        )

        overlayManager.safeAddView(windowManager, composeView, headParams)
        return holder
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun attachDragTouchListener(
        context: Context,
        service: ChatHeadService,
        windowManager: WindowManager,
        holder: ChatHeadHolder,
        onTap: () -> Unit,
        onRemove: () -> Unit
    ) {
        var initialX        = 0
        var initialY        = 0
        var initialTouchX   = 0f
        var initialTouchY   = 0f
        var isDragging      = false
        var trashView: View? = null

        holder.headView.setOnTouchListener { _, event ->
            val screenW = context.resources.displayMetrics.widthPixels
            val screenH = context.resources.displayMetrics.heightPixels

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX      = holder.headParams.x
                    initialY      = holder.headParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging    = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()

                    if (!isDragging && (kotlin.math.abs(dx) > 10 || kotlin.math.abs(dy) > 10)) {
                        isDragging = true
                        trashView  = showTrashDropZone(context, service, windowManager)
                    }

                    if (isDragging) {
                        holder.headParams.x = (initialX + dx).coerceIn(16, screenW - 140)
                        holder.headParams.y = (initialY + dy).coerceIn(50, screenH - 200)
                        overlayManager.safeUpdateViewLayout(windowManager, holder.headView, holder.headParams)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    trashView?.let { overlayManager.safeRemoveView(windowManager, it); trashView = null }

                    if (!isDragging) {
                        onTap()
                    } else {
                        // Check if released inside Trash Drop Zone (bottom 160px of screen)
                        if (event.rawY > (screenH - 160)) {
                            Timber.d("ChatHeadManager: Head released inside trash drop zone — removing convId=${holder.convId}")
                            onRemove()
                        } else {
                            // Snap to nearest screen edge (left or right)
                            snapToScreenEdge(context, windowManager, holder)
                        }
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun showTrashDropZone(context: Context, service: ChatHeadService, windowManager: WindowManager): View {
        val trashParams = overlayManager.createHeadLayoutParams(0, 0).apply {
            width   = WindowManager.LayoutParams.MATCH_PARENT
            height  = WindowManager.LayoutParams.WRAP_CONTENT
            gravity = android.view.Gravity.BOTTOM or android.view.Gravity.CENTER_HORIZONTAL
        }
        val trashView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(service)
            setViewTreeViewModelStoreOwner(service)
            setViewTreeSavedStateRegistryOwner(service)
            setContent {
                MeshTheme {
                    TrashDropZone()
                }
            }
        }
        overlayManager.safeAddView(windowManager, trashView, trashParams)
        return trashView
    }

    private fun snapToScreenEdge(context: Context, windowManager: WindowManager, holder: ChatHeadHolder) {
        val screenW   = context.resources.displayMetrics.widthPixels
        val targetX   = if (holder.headParams.x < screenW / 2) 16 else (screenW - 140)
        val startX    = holder.headParams.x

        val animator = ValueAnimator.ofInt(startX, targetX).apply {
            duration     = 220L
            interpolator = DecelerateInterpolator()
            addUpdateListener { anim ->
                holder.headParams.x = anim.animatedValue as Int
                overlayManager.safeUpdateViewLayout(windowManager, holder.headView, holder.headParams)
            }
        }
        animator.start()
    }
}
