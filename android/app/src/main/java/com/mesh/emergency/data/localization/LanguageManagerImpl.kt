/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.localization

import com.mesh.emergency.core.localization.LanguageManager
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [LanguageManager] coordinating locale updates and numeric conversions.
 */
@Singleton
class LanguageManagerImpl @Inject constructor() : LanguageManager {

    private var activeLanguage = "en"

    override fun getCurrentLanguage(): String = activeLanguage

    override fun changeLanguage(langCode: String) {
        activeLanguage = if (langCode == "bn") "bn" else "en"
    }

    override fun getSystemLanguage(): String {
        val sysLang = Locale.getDefault().language
        return if (sysLang == "bn") "bn" else "en"
    }

    override fun formatNumber(number: Double): String {
        val locale = Locale(activeLanguage)
        val formatter = NumberFormat.getInstance(locale)
        return formatter.format(number)
    }

    override fun formatDate(timestamp: Long): String {
        val locale = Locale(activeLanguage)
        val formatPattern = if (activeLanguage == "bn") "dd/MM/yyyy HH:mm" else "yyyy-MM-dd HH:mm"
        val sdf = SimpleDateFormat(formatPattern, locale)
        return sdf.format(Date(timestamp))
    }
}
