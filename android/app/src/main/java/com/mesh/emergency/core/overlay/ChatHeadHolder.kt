/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.overlay

import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.MutableState
import androidx.compose.ui.platform.ComposeView

/**
 * Holder representing a single active floating Chat Head on the screen.
 */
data class ChatHeadHolder(
    val convId: String,
    var label: String,
    val headView: View,
    val headParams: WindowManager.LayoutParams,
    val unreadState: MutableState<Int>,
    val pulseState: MutableState<Boolean>,
    var popupView: ComposeView? = null,
    var popupParams: WindowManager.LayoutParams? = null
)
