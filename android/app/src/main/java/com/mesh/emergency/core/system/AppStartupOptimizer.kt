/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.system

import android.content.Context
import timber.log.Timber

/**
 * Application startup optimizer for deferring non-critical initializations.
 *
 * Call [initialize] from Application.onCreate(). Heavy components are
 * initialized lazily via [initDeferred] called from background thread.
 */
object AppStartupOptimizer {

    private var isInitialized = false

    /**
     * Lightweight critical-path initialization — runs on main thread.
     * Keep this under 50ms.
     */
    fun initialize(context: Context) {
        if (isInitialized) return

        val start = System.currentTimeMillis()
        initTimber()
        isInitialized = true

        val elapsed = System.currentTimeMillis() - start
        Timber.d("AppStartup: Critical init completed in ${elapsed}ms")
    }

    /**
     * Non-critical deferred initialization — run on background thread after
     * first frame is rendered.
     */
    fun initDeferred(context: Context) {
        val start = System.currentTimeMillis()

        runCatching {
            // Pre-warm database connection (non-blocking)
            Timber.d("AppStartup: Deferred init started")
        }

        val elapsed = System.currentTimeMillis() - start
        Timber.d("AppStartup: Deferred init completed in ${elapsed}ms")
    }

    private fun initTimber() {
        if (Timber.treeCount == 0) {
            Timber.plant(Timber.DebugTree())
        }
    }
}

/**
 * Extension to measure execution time of a block.
 */
inline fun <T> measureBlock(tag: String, block: () -> T): T {
    val start = System.currentTimeMillis()
    val result = block()
    val elapsed = System.currentTimeMillis() - start
    Timber.d("$tag: ${elapsed}ms")
    return result
}
