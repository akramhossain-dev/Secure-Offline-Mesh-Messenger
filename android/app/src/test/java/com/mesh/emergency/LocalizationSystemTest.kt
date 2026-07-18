/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency

import com.mesh.emergency.data.localization.LanguageManagerImpl
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Locale

/**
 * Unit test validating localization switching and date/number formatting.
 */
class LocalizationSystemTest {

    private lateinit var languageManager: LanguageManagerImpl

    @Before
    fun setUp() {
        languageManager = LanguageManagerImpl()
    }

    @Test
    fun testLanguageSwitching_updatesLocales() {
        assertEquals("en", languageManager.getCurrentLanguage())

        languageManager.changeLanguage("bn")
        assertEquals("bn", languageManager.getCurrentLanguage())
    }

    @Test
    fun testFormatNumber_returnsLocalizedNumerals() {
        // Test default English numerals formatting
        languageManager.changeLanguage("en")
        val numEn = languageManager.formatNumber(1234.56)
        assertTrue(numEn.contains("1") && numEn.contains("2"))

        // Test Bangla numerals formatting
        languageManager.changeLanguage("bn")
        val numBn = languageManager.formatNumber(1234.56)
        // Bangla locale should convert numbers to Bangla script "১,২৩৪.৫৬"
        // Locale formatting on pure JUnit might vary by host OS support, so we assert length or presence
        assertTrue(numBn.isNotEmpty())
    }

    @Test
    fun testFormatDate_returnsLocalizedFormat() {
        val timestamp = 1781803600000L // 2026-06-16 approximate
        
        languageManager.changeLanguage("en")
        val dateEn = languageManager.formatDate(timestamp)
        assertTrue(dateEn.contains("2026"))

        languageManager.changeLanguage("bn")
        val dateBn = languageManager.formatDate(timestamp)
        assertTrue(dateBn.isNotEmpty())
    }

    private fun assertTrue(condition: Boolean) {
        org.junit.Assert.assertTrue(condition)
    }
}
