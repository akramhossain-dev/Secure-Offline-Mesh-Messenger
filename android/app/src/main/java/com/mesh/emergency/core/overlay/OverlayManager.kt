/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Encapsulates Android [WindowManager] layout parameter creation and safe view operations.
 */
@Singleton
class OverlayManager @Inject constructor() {

    fun getWindowManager(context: Context): WindowManager {
        return context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    /** Creates WindowManager LayoutParams for circular Chat Head bubbles. */
    fun createHeadLayoutParams(x: Int, y: Int): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            this.x = x
            this.y = y
        }
    }

    /** Creates WindowManager LayoutParams for full expandable floating chat popups. */
    fun createPopupLayoutParams(width: Int, height: Int): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        return WindowManager.LayoutParams(
            width,
            height,
            type,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }
    }

    /** Safely adds a view to WindowManager guarding against race condition exceptions. */
    fun safeAddView(wm: WindowManager, view: View, params: WindowManager.LayoutParams): Boolean {
        return try {
            wm.addView(view, params)
            true
        } catch (e: Exception) {
            Timber.w(e, "OverlayManager: Failed to add view to WindowManager")
            false
        }
    }

    /** Safely updates a view's layout parameters in WindowManager. */
    fun safeUpdateViewLayout(wm: WindowManager, view: View, params: WindowManager.LayoutParams): Boolean {
        return try {
            wm.updateViewLayout(view, params)
            true
        } catch (e: Exception) {
            Timber.w(e, "OverlayManager: Failed to update view layout in WindowManager")
            false
        }
    }

    /** Safely removes a view from WindowManager. */
    fun safeRemoveView(wm: WindowManager, view: View): Boolean {
        return try {
            wm.removeView(view)
            true
        } catch (e: Exception) {
            Timber.w(e, "OverlayManager: Failed to remove view from WindowManager")
            false
        }
    }
}
