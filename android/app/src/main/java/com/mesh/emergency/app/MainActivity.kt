/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.mesh.emergency.core.designsystem.theme.MeshTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main entry-point Activity for the Offline Emergency Mesh Communication System.
 *
 * Phase A1: Sets up the Compose content host with [MeshTheme].
 * Phase A2+: Will host the NavHost and navigation graph.
 *
 * Edge-to-edge is enabled for immersive UI.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen before super.onCreate()
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MeshTheme {
                // Phase A1: Placeholder surface.
                // Phase A2 will replace this with NavHost + full navigation graph.
                Surface(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    // Navigation host will be placed here in Phase A2
                }
            }
        }
    }
}
