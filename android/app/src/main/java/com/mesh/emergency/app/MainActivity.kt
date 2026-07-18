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
import com.mesh.emergency.core.designsystem.theme.MeshTheme
import com.mesh.emergency.core.navigation.AppNavigator
import com.mesh.emergency.core.presentation.AppShell
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Main entry-point Activity for the Offline Emergency Mesh Communication System.
 * Hosts the responsive [AppShell] to coordinate navigation and adaptive UI layouts.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /** Injected navigation coordinator singleton. */
    @Inject
    lateinit var appNavigator: AppNavigator

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen before super.onCreate()
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MeshTheme {
                AppShell(appNavigator = appNavigator)
            }
        }
    }
}
