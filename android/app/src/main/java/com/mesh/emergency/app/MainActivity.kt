/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import com.mesh.emergency.core.designsystem.theme.MeshTheme
import com.mesh.emergency.core.navigation.AppNavigator
import com.mesh.emergency.core.presentation.AppShell
import com.mesh.emergency.core.domain.AppStateRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Main entry-point Activity for the Offline Emergency Mesh Communication System.
 * Hosts the responsive [AppShell] to coordinate navigation and adaptive UI layouts.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    /** Injected navigation coordinator singleton. */
    @Inject
    lateinit var appNavigator: AppNavigator

    /** Injected global application state repository. */
    @Inject
    lateinit var appStateRepository: AppStateRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen before super.onCreate()
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val appState by appStateRepository.appState.collectAsState()

            // Dynamic and persistent locale update
            LaunchedEffect(appState.languageCode) {
                val localeList = if (appState.languageCode == "system" || appState.languageCode.isEmpty()) {
                    LocaleListCompat.getEmptyLocaleList()
                } else {
                    LocaleListCompat.forLanguageTags(appState.languageCode)
                }
                if (AppCompatDelegate.getApplicationLocales() != localeList) {
                    AppCompatDelegate.setApplicationLocales(localeList)
                }
            }

            MeshTheme(themeMode = appState.themeMode) {
                AppShell(appNavigator = appNavigator)
            }
        }
    }
}
