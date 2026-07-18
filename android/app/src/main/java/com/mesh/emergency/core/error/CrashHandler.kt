/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.error

import android.content.Context
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Global uncaught exception handler for crash capture and local persistence.
 *
 * Writes full crash reports to [Context.filesDir]/logs/crash.log (append mode).
 * File is capped at [MAX_LOG_BYTES] — oldest content is rotated on overflow.
 * Logs crashes to Timber before delegating to the default JVM handler.
 *
 * Install via [install] from Application.onCreate() AFTER Timber is initialized.
 */
class CrashHandler private constructor(
    private val context: Context,
    private val defaultHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    private val logFile: File by lazy {
        File(context.filesDir, "logs/crash.log").also { it.parentFile?.mkdirs() }
    }

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
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val report = buildString {
            appendLine("=== CRASH REPORT [$timestamp] ===")
            appendLine("Thread : ${thread.name}")
            appendLine("Type   : ${throwable::class.qualifiedName}")
            appendLine("Message: ${throwable.message}")
            appendLine("Stack  :")
            throwable.stackTrace.take(20).forEach { appendLine("  at $it") }
            throwable.cause?.let { cause ->
                appendLine("Caused by: ${cause::class.simpleName}: ${cause.message}")
                cause.stackTrace.take(10).forEach { appendLine("  at $it") }
            }
            appendLine("=== END CRASH REPORT ===\n")
        }

        try {
            // Rotate file if it exceeds MAX_LOG_BYTES
            if (logFile.exists() && logFile.length() > MAX_LOG_BYTES) {
                val existing = logFile.readText()
                val trimmed = existing.drop(existing.length / 2)
                logFile.writeText(trimmed)
            }
            logFile.appendText(report)
        } catch (e: Exception) {
            Timber.e(e, "CrashHandler: Failed to write crash log")
        }
    }

    companion object {
        private const val MAX_LOG_BYTES = 512_000L // 500 KB cap
        private var isInstalled = false

        /**
         * Installs [CrashHandler] as the global uncaught exception handler.
         * Safe to call multiple times — only installs once.
         *
         * @param context Application context used to resolve the log file path.
         */
        fun install(context: Context) {
            if (isInstalled) return
            val existing = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler(CrashHandler(context, existing))
            isInstalled = true
            Timber.d("CrashHandler: Installed — crash log → ${context.filesDir}/logs/crash.log")
        }
    }
}
