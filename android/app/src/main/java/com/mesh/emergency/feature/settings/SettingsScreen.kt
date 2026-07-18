/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mesh.emergency.core.designsystem.component.AuroraBackdrop
import com.mesh.emergency.core.designsystem.component.GlassPanel
import com.mesh.emergency.core.designsystem.component.GlassPanelVariant
import com.mesh.emergency.core.designsystem.component.MeshOutlinedButton
import com.mesh.emergency.core.designsystem.theme.MeshThemeTokens
import com.mesh.emergency.core.designsystem.theme.ThemeMode

/**
 * Settings screen — theme, language, privacy settings, debug mode, log management.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val spacing = MeshThemeTokens.spacing

    // Confirmation dialog states
    var showConfirmDialog by remember { mutableStateOf(false) }
    var confirmTitle by remember { mutableStateOf("") }
    var confirmMessage by remember { mutableStateOf("") }
    var pendingEvent by remember { mutableStateOf<SettingsUiEvent?>(null) }

    val triggerAction = { title: String, message: String, event: SettingsUiEvent ->
        confirmTitle = title
        confirmMessage = message
        pendingEvent = event
        showConfirmDialog = true
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is SettingsUiEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    AuroraBackdrop(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text("Settings", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { paddingValues ->
            LazyColumn(
                contentPadding = PaddingValues(
                    start = spacing.lg, end = spacing.lg,
                    top = paddingValues.calculateTopPadding() + spacing.sm,
                    bottom = paddingValues.calculateBottomPadding() + spacing.lg
                ),
                verticalArrangement = Arrangement.spacedBy(spacing.md),
                modifier = Modifier.fillMaxSize()
            ) {

                // ── Appearance ────────────────────────────────────────────────
                item {
                    SettingsSectionHeader("Appearance")
                }
                item {
                    GlassPanel(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text("Theme Mode", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                            Spacer(Modifier.height(spacing.sm))
                            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                                ThemeMode.entries.forEach { mode ->
                                    FilterChip(
                                        selected = uiState.themeMode == mode,
                                        onClick = { viewModel.onEvent(SettingsUiEvent.ChangeTheme(mode)) },
                                        label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) }
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Language ──────────────────────────────────────────────────
                item { SettingsSectionHeader("Language") }
                item {
                    GlassPanel(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text("Display Language", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                            Spacer(Modifier.height(spacing.sm))
                            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                                listOf("en" to "English", "bn" to "বাংলা").forEach { (code, label) ->
                                    FilterChip(
                                        selected = uiState.languageCode == code,
                                        onClick = { viewModel.onEvent(SettingsUiEvent.ChangeLanguage(code)) },
                                        label = { Text(label) }
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Privacy & Security ────────────────────────────────────────
                item { SettingsSectionHeader("Privacy & Security") }
                item {
                    GlassPanel(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("End-to-End Encryption", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                                    Text("AES-256-GCM + ECDH keys", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(checked = uiState.encryptionEnabled, onCheckedChange = null, enabled = false)
                            }
                            
                            Spacer(Modifier.height(4.dp))
                            
                            MeshOutlinedButton(
                                text = "Delete All Messages",
                                onClick = {
                                    triggerAction(
                                        "Delete All Messages?",
                                        "This will permanently erase all text and voice messages from local storage. This action cannot be undone.",
                                        SettingsUiEvent.DeleteAllMessages
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            )

                            MeshOutlinedButton(
                                text = "Remove Paired Devices",
                                onClick = {
                                    triggerAction(
                                        "Remove Paired Devices?",
                                        "This will clear the cached list of discovered devices and paired nodes.",
                                        SettingsUiEvent.RemoveTrustedDevices
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            )

                            MeshOutlinedButton(
                                text = "Rotate Cryptographic Keys",
                                onClick = {
                                    triggerAction(
                                        "Rotate Cryptographic Keys?",
                                        "This will invalidate your current identity key pairs and generate a fresh pair. You will need to re-pair with contacts.",
                                        SettingsUiEvent.ResetSecurityKeys
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            )

                            MeshOutlinedButton(
                                text = "Wipe All Application Data",
                                onClick = {
                                    triggerAction(
                                        "Wipe All Local Data?",
                                        "This will delete all messages, locations, resource shares, trusted devices, and cryptographic identity keys. The app will restart in a clean slate state.",
                                        SettingsUiEvent.ClearLocalData
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // ── Debug ─────────────────────────────────────────────────────
                item { SettingsSectionHeader("Developer") }
                item {
                    GlassPanel(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Debug Mode", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                                    Text("Show verbose logs", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = uiState.debugModeEnabled,
                                    onCheckedChange = { viewModel.onEvent(SettingsUiEvent.ToggleDebugMode(it)) }
                                )
                            }
                            Spacer(Modifier.height(spacing.md))
                            MeshOutlinedButton(
                                text = "Clear Diagnostic Logs",
                                onClick = { viewModel.onEvent(SettingsUiEvent.ClearLogs) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // ── About ─────────────────────────────────────────────────────
                item { SettingsSectionHeader("About") }
                item {
                    GlassPanel(modifier = Modifier.fillMaxWidth(), variant = GlassPanelVariant.DEFAULT) {
                        Column {
                            SettingsInfoRow("App Version", uiState.appVersion)
                            SettingsInfoRow("Build", "Debug")
                            SettingsInfoRow("Architecture", "Clean Architecture + MVI")
                            SettingsInfoRow("Storage Mode", "Offline — Room Database")
                        }
                    }
                }
            }
        }
    }

    // Confirmation dialog
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text(confirmTitle, fontWeight = FontWeight.Bold) },
            text = { Text(confirmMessage) },
            confirmButton = {
                Button(
                    onClick = {
                        pendingEvent?.let { viewModel.onEvent(it) }
                        showConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Proceed")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun SettingsInfoRow(label: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.onSurface)
    }
    Spacer(Modifier.height(MeshThemeTokens.spacing.sm))
}
