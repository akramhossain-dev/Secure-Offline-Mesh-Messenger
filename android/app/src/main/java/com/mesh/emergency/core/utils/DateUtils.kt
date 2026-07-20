/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Date and time formatting utilities for the Offline Emergency Mesh system.
 *
 * All formatting is done locally — no network time dependency.
 * Formats are designed for emergency communication contexts where
 * precise, readable timestamps are critical.
 */
object DateUtils {

    // ─────────────────────────────────────────────────────────────────────────
    // Format patterns
    // ─────────────────────────────────────────────────────────────────────────

    private const val PATTERN_MESSAGE_TIME  = "HH:mm"
    private const val PATTERN_MESSAGE_DATE  = "MMM dd, yyyy"
    private const val PATTERN_FULL          = "MMM dd, yyyy HH:mm"
    private const val PATTERN_ISO8601       = "yyyy-MM-dd'T'HH:mm:ss"
    private const val PATTERN_FILE          = "yyyy-MM-dd_HH-mm-ss"

    // ─────────────────────────────────────────────────────────────────────────
    // Formatting
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Formats a Unix timestamp [millis] as a short time string ("HH:mm").
     * Used in chat message timestamps.
     */
    fun formatMessageTime(millis: Long, locale: Locale = Locale.getDefault()): String =
        SimpleDateFormat(PATTERN_MESSAGE_TIME, locale).format(Date(millis))

    /**
     * Formats a Unix timestamp [millis] dynamically using the device's 12/24 hour settings.
     */
    fun formatMessageTime(context: android.content.Context, millis: Long): String {
        val is24Hour = android.text.format.DateFormat.is24HourFormat(context)
        val pattern = if (is24Hour) "HH:mm" else "h:mm a"
        return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(millis))
    }

    /**
     * Returns a relative day string (Today, Yesterday, Day Name, or Full Date).
     */
    fun formatRelativeDay(millis: Long): String {
        val now = System.currentTimeMillis()
        val calendar = java.util.Calendar.getInstance().apply { timeInMillis = millis }
        val nowCalendar = java.util.Calendar.getInstance().apply { timeInMillis = now }

        val isToday = calendar.get(java.util.Calendar.YEAR) == nowCalendar.get(java.util.Calendar.YEAR) &&
                calendar.get(java.util.Calendar.DAY_OF_YEAR) == nowCalendar.get(java.util.Calendar.DAY_OF_YEAR)

        val isYesterday = calendar.get(java.util.Calendar.YEAR) == nowCalendar.get(java.util.Calendar.YEAR) &&
                calendar.get(java.util.Calendar.DAY_OF_YEAR) == nowCalendar.get(java.util.Calendar.DAY_OF_YEAR) - 1

        return when {
            isToday -> "Today"
            isYesterday -> "Yesterday"
            else -> {
                val diffMillis = now - millis
                val diffDays = diffMillis / (24 * 60 * 60 * 1000)
                if (diffDays in 0..6) {
                    SimpleDateFormat("EEEE", Locale.getDefault()).format(Date(millis))
                } else {
                    SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(millis))
                }
            }
        }
    }

    /**
     * Formats a Unix timestamp [millis] as a date string ("MMM dd, yyyy").
     * Used in chat date separators.
     */
    fun formatMessageDate(millis: Long, locale: Locale = Locale.getDefault()): String =
        SimpleDateFormat(PATTERN_MESSAGE_DATE, locale).format(Date(millis))

    /**
     * Formats a Unix timestamp [millis] as a full datetime string.
     */
    fun formatFull(millis: Long, locale: Locale = Locale.getDefault()): String =
        SimpleDateFormat(PATTERN_FULL, locale).format(Date(millis))

    /**
     * Formats a Unix timestamp [millis] as ISO 8601 string.
     * Used for packet serialization.
     */
    fun formatIso8601(millis: Long): String =
        SimpleDateFormat(PATTERN_ISO8601, Locale.US).format(Date(millis))

    /**
     * Formats a Unix timestamp [millis] as a filesystem-safe string.
     * Used for log file and export naming.
     */
    fun formatForFilename(millis: Long): String =
        SimpleDateFormat(PATTERN_FILE, Locale.US).format(Date(millis))

    // ─────────────────────────────────────────────────────────────────────────
    // Relative time
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns a human-readable relative time string for [millis].
     *
     * Examples:
     * - "Just now" (< 60 seconds ago)
     * - "5 min ago"
     * - "2 hours ago"
     * - "Yesterday"
     * - "MMM dd, yyyy" (older)
     */
    fun formatRelative(millis: Long, now: Long = System.currentTimeMillis()): String {
        val delta = now - millis
        return when {
            delta < TimeUnit.MINUTES.toMillis(1)    -> "Just now"
            delta < TimeUnit.HOURS.toMillis(1)      -> "${TimeUnit.MILLISECONDS.toMinutes(delta)} min ago"
            delta < TimeUnit.HOURS.toMillis(24)     -> "${TimeUnit.MILLISECONDS.toHours(delta)} h ago"
            delta < TimeUnit.HOURS.toMillis(48)     -> "Yesterday"
            else                                     -> formatMessageDate(millis)
        }
    }

    /**
     * Returns `true` if two timestamps [a] and [b] fall on the same calendar day.
     */
    fun isSameDay(a: Long, b: Long): Boolean {
        val fmt = SimpleDateFormat("yyyyMMdd", Locale.US)
        return fmt.format(Date(a)) == fmt.format(Date(b))
    }

    /**
     * Returns the current Unix timestamp in milliseconds.
     */
    fun now(): Long = System.currentTimeMillis()

    /**
     * Formats a duration in [millis] as "mm:ss".
     * Used for voice message duration display.
     */
    fun formatDuration(millis: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        return "%02d:%02d".format(minutes, seconds)
    }
}
