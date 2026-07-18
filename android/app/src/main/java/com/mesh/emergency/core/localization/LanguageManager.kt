/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.localization

/**
 * Interface contract coordinating dynamic language preference changes and formats.
 */
interface LanguageManager {
    /** Returns current selected language code (e.g. en, bn). */
    fun getCurrentLanguage(): String

    /** Changes active language settings. */
    fun changeLanguage(langCode: String)

    /** Audits current OS system language code. */
    fun getSystemLanguage(): String

    /** Formats a numeric value into locale-specific characters. */
    fun formatNumber(number: Double): String

    /** Formats an epoch timestamp into locale-specific date/time strings. */
    fun formatDate(timestamp: Long): String
}
