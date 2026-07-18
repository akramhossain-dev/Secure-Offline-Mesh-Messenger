/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.common.localization

/**
 * Enumeration of all supported languages in the Offline Emergency Mesh application.
 *
 * Users can manually select a language in Settings, overriding the system locale.
 * The preference is persisted in DataStore and applied at app startup.
 *
 * Phase A2: A LocaleViewModel will apply this preference when the app starts,
 * calling [LocaleManager.applyLocale].
 */
enum class SupportedLanguage(
    /** BCP 47 language tag used for Locale creation and resource matching. */
    val tag: String,
    /** Native display name shown in the language picker UI. */
    val displayName: String,
    /** Fallback English name for accessibility and logging. */
    val englishName: String,
) {
    /** Follow the device's system language. No manual override is applied. */
    SYSTEM(
        tag         = "",
        displayName = "System Default",
        englishName = "System Default",
    ),

    /** English (United States). Default fallback language. */
    ENGLISH(
        tag         = "en",
        displayName = "English",
        englishName = "English",
    ),

    /** Bengali / Bangla. */
    BANGLA(
        tag         = "bn",
        displayName = "বাংলা",
        englishName = "Bangla",
    );

    companion object {

        /** The default language applied on first launch. */
        val DEFAULT: SupportedLanguage = SYSTEM

        /**
         * Returns the [SupportedLanguage] matching the given BCP 47 [tag],
         * or [DEFAULT] if not found.
         */
        fun fromTag(tag: String): SupportedLanguage =
            entries.firstOrNull { it.tag.equals(tag, ignoreCase = true) } ?: DEFAULT

        /**
         * Returns the [SupportedLanguage] matching the given persisted [name],
         * or [DEFAULT] if not found.
         */
        fun fromString(name: String): SupportedLanguage =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: DEFAULT
    }
}
