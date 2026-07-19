/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.common.localization

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mesh.emergency.core.common.constants.AppConstants
import com.mesh.emergency.core.common.logging.AppLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the application's locale/language preference.
 *
 * Phase A1: Infrastructure only — defines the locale management contract.
 * Phase A2: This will be injected into a SettingsViewModel to expose
 *           the language picker and apply locale changes.
 *
 * Strategy:
 * - Android 13+ (API 33): Uses [AppCompatDelegate.setApplicationLocales] (per-app locale).
 * - Android < 13: Applies locale via [Context.createConfigurationContext] and
 *   [AppCompatDelegate.setApplicationLocales] fallback via AppCompat.
 *
 * The selected language is persisted in DataStore and re-applied on app restart.
 *
 * @param dataStore The application-scoped DataStore instance, injected by Hilt.
 */
@Singleton
class LocaleManager @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {

    private val TAG = "LocaleManager"
    private val keyLanguage = stringPreferencesKey("app_language_code")

    // ─────────────────────────────────────────────────────────────────────────
    // Observe preference
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * A [Flow] emitting the currently persisted [SupportedLanguage].
     * Emits [SupportedLanguage.DEFAULT] if no preference has been set.
     */
    val selectedLanguage: Flow<SupportedLanguage> = dataStore.data.map { prefs ->
        SupportedLanguage.fromString(prefs[keyLanguage] ?: SupportedLanguage.DEFAULT.name)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Persist + apply
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Persists the selected [language] to DataStore and applies it immediately
     * via [AppCompatDelegate.setApplicationLocales].
     *
     * Must be called from a coroutine scope.
     */
    suspend fun setLanguage(language: SupportedLanguage) {
        AppLogger.d(TAG, "Setting language to: ${language.name} (${language.tag})")
        dataStore.edit { prefs ->
            prefs[keyLanguage] = language.name
        }
        applyLocale(language)
    }

    /**
     * Applies the given [language] to the running app instance.
     *
     * For [SupportedLanguage.SYSTEM], resets to the device locale.
     * For all other languages, applies the BCP-47 tag.
     *
     * This uses AppCompat's per-app locale API which handles activity recreation
     * and Android 13+ per-app locale settings automatically.
     */
    fun applyLocale(language: SupportedLanguage) {
        val localeList = if (language == SupportedLanguage.SYSTEM) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(language.tag)
        }
        AppCompatDelegate.setApplicationLocales(localeList)
        AppLogger.d(TAG, "Locale applied: ${language.tag.ifEmpty { "system" }}")
    }

    /**
     * Returns the currently active [Locale] for the application.
     * If the persisted language is SYSTEM, returns the device default locale.
     */
    fun getActiveLocale(language: SupportedLanguage): Locale =
        if (language == SupportedLanguage.SYSTEM || language.tag.isEmpty()) {
            Locale.getDefault()
        } else {
            Locale.forLanguageTag(language.tag)
        }
}
