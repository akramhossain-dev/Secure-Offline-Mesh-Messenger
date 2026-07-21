/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.overlay

import android.content.Context
import android.content.Intent
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.mesh.emergency.app.MainActivity
import com.mesh.emergency.core.communication.CommunicationManager
import com.mesh.emergency.core.designsystem.theme.MeshTheme
import com.mesh.emergency.core.messaging.MessagingService
import com.mesh.emergency.core.navigation.NavRoutes
import com.mesh.emergency.data.local.LocalDataSource
import com.mesh.emergency.feature.overlay.OverlayChatScreen
import com.mesh.emergency.feature.overlay.OverlayConversationViewModel
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages floating expandable chat popups, isolated [ViewModelStore] lifecycle scoping,
 * and full-app navigation dispatching.
 */
@Singleton
class PopupConversationController @Inject constructor(
    private val overlayManager: OverlayManager,
    private val localDataSource: LocalDataSource,
    private val messagingService: MessagingService,
    private val communicationManager: CommunicationManager
) {

    /**
     * Toggles (opens/closes) the floating chat popup window for a conversation.
     */
    fun togglePopup(
        context: Context,
        service: ChatHeadService,
        windowManager: WindowManager,
        holder: ChatHeadHolder,
        convViewModelStores: MutableMap<String, ViewModelStore>,
        onCloseRequested: () -> Unit
    ) {
        if (holder.popupView != null) {
            closePopup(service, windowManager, holder)
        } else {
            openPopup(context, service, windowManager, holder, convViewModelStores, onCloseRequested)
        }
    }

    /** Opens an expandable floating chat window. */
    fun openPopup(
        context: Context,
        service: ChatHeadService,
        windowManager: WindowManager,
        holder: ChatHeadHolder,
        convViewModelStores: MutableMap<String, ViewModelStore>,
        onCloseRequested: () -> Unit
    ) {
        if (holder.popupView != null) return

        // Stop pulsing badge when opened
        holder.pulseState.value  = false
        holder.unreadState.value = 0

        val screenW = context.resources.displayMetrics.widthPixels
        val screenH = context.resources.displayMetrics.heightPixels

        val popupWidth  = (screenW * 0.94f).toInt()
        val popupHeight = (screenH * 0.87f).toInt()

        val pParams = overlayManager.createPopupLayoutParams(popupWidth, popupHeight)

        // Obtain or create isolated ViewModelStore for this conversation
        val vmStore = convViewModelStores.getOrPut(holder.convId) { ViewModelStore() }
        val storeOwner = object : ViewModelStoreOwner {
            override val viewModelStore: ViewModelStore = vmStore
        }

        // Instantiate OverlayConversationViewModel with dependencies
        val vm = OverlayConversationViewModel(localDataSource, messagingService, communicationManager)
        vm.init(holder.convId, holder.label)

        val composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(service)
            setViewTreeViewModelStoreOwner(storeOwner)
            setViewTreeSavedStateRegistryOwner(service)
            setContent {
                MeshTheme {
                    OverlayChatScreen(
                        viewModel = vm,
                        activeHeads = service.getActiveHeads(),
                        activeConvId = holder.convId,
                        onSelectHead = { targetId -> service.switchActivePopup(targetId) },
                        onClose = {
                            closePopup(service, windowManager, holder)
                            onCloseRequested()
                        },
                        onExpand = {
                            expandToFullApp(context, holder.convId, holder.label)
                            closePopup(service, windowManager, holder)
                            onCloseRequested()
                        }
                    )
                }
            }
        }

        holder.popupView   = composeView
        holder.popupParams = pParams

        overlayManager.safeAddView(windowManager, composeView, pParams)
        service.setFloatingHeadsVisible(false)
        Timber.d("PopupConversationController: Opened chat popup convId=${holder.convId}")
    }

    /** Closes the expandable floating chat window for a conversation. */
    fun closePopup(service: ChatHeadService, windowManager: WindowManager, holder: ChatHeadHolder) {
        holder.popupView?.let { view ->
            overlayManager.safeRemoveView(windowManager, view)
            holder.popupView   = null
            holder.popupParams = null
            Timber.d("PopupConversationController: Closed chat popup convId=${holder.convId}")
        }
        // Restore floating heads visibility if no other popup is open
        service.setFloatingHeadsVisible(true)
    }

    /** Launches MainActivity navigated directly to the full conversation route. */
    private fun expandToFullApp(context: Context, convId: String, label: String) {
        val targetRoute = if (convId == "global") NavRoutes.GLOBAL_CHAT else NavRoutes.chatScreen(convId, label)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_CONV_ID, convId)
            putExtra(MainActivity.EXTRA_LABEL, label)
            putExtra(MainActivity.EXTRA_NAV_ROUTE, targetRoute)
        }
        context.startActivity(intent)
        Timber.d("PopupConversationController: Expanded to full app route=$targetRoute")
    }
}
