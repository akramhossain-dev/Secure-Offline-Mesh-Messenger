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

import kotlinx.coroutines.flow.first

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

    @Inject
    lateinit var messageNotifier: com.mesh.emergency.core.notification.MessageNotifier

    private val requestPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        val permissionsList = mutableListOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            permissionsList.add(android.Manifest.permission.BLUETOOTH_SCAN)
            permissionsList.add(android.Manifest.permission.BLUETOOTH_CONNECT)
            permissionsList.add(android.Manifest.permission.BLUETOOTH_ADVERTISE)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissionsList.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        requestPermissionLauncher.launch(permissionsList.toTypedArray())

        // Install splash screen before super.onCreate()
        installSplashScreen()
        org.maplibre.android.MapLibre.getInstance(this)
        

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIntent(intent)

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
                AppShell(appNavigator = appNavigator, messageNotifier = messageNotifier)
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: android.content.Intent?) {
        intent ?: return
        val convId   = intent.getStringExtra("convId")
        val label    = intent.getStringExtra("label") ?: "Chat"
        val navRoute = intent.getStringExtra("navRoute")

        val targetConvId = when {
            !convId.isNullOrEmpty() -> convId
            navRoute == com.mesh.emergency.core.navigation.NavRoutes.GLOBAL_CHAT -> "global"
            else -> null
        }

        if (targetConvId != null) {
            // Dismiss chat head for this conversation since the full app is opening it
            try {
                val removeIntent = android.content.Intent(this, com.mesh.emergency.core.overlay.ChatHeadService::class.java).apply {
                    action = com.mesh.emergency.core.overlay.ChatHeadService.ACTION_REMOVE_HEAD
                    putExtra(com.mesh.emergency.core.overlay.ChatHeadService.EXTRA_CONV_ID, targetConvId)
                }
                androidx.core.content.ContextCompat.startForegroundService(this, removeIntent)
            } catch (e: Exception) {
                // Ignore if service is not running
            }

            if (targetConvId == "global") {
                appNavigator.navigateTo(com.mesh.emergency.core.navigation.NavRoutes.GLOBAL_CHAT)
            } else {
                appNavigator.navigateTo(com.mesh.emergency.core.navigation.NavRoutes.chatScreen(targetConvId, label))
            }
        }
    }

    companion object {
        const val EXTRA_CONV_ID   = "convId"
        const val EXTRA_LABEL     = "label"
        const val EXTRA_NAV_ROUTE = "navRoute"
    }
}

