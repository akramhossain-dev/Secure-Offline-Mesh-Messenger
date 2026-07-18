/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.error

import timber.log.Timber

/**
 * Global uncaught exception handler for crash capture and recovery.
 *
 * Logs crashes to Timber before delegating to the default JVM handler.
 * In a production build this would additionally write to local DB.
 *
 * Install via [install] from Application.onCreate() AFTER Timber is initialized.
 */
class CrashHandler private constructor(
    private val defaultHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            Timber.e(throwable, "CRASH: Uncaught exception on thread '${thread.name}'")
            logCrashLocally(thread, throwable)
        } catch (e: Exception) {
            // Swallow secondary exceptions during crash logging
        } finally {
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun logCrashLocally(thread: Thread, throwable: Throwable) {
        // In a production build: write crash report to Room DB via a
        // non-blocking background thread. Currently logs to Timber only.
        val report = buildString {
            appendLine("=== CRASH REPORT ===")
            appendLine("Thread: ${thread.name}")
            appendLine("Message: ${throwable.message}")
            appendLine("Type: ${throwable::class.simpleName}")
            appendLine("Stack:")
            throwable.stackTrace.take(10).forEach { appendLine("  at $it") }
            appendLine("===================")
        }
        Timber.e(report)
    }

    companion object {
        private var isInstalled = false

        /**
         * Installs [CrashHandler] as the global uncaught exception handler.
         * Safe to call multiple times — only installs once.
         */
        fun install() {
            if (isInstalled) return
            val existing = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler(CrashHandler(existing))
            isInstalled = true
            Timber.d("CrashHandler: Installed successfully")
        }
    }
}
