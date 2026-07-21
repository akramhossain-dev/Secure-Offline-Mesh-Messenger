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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mesh.emergency.R
import com.mesh.emergency.core.designsystem.component.*
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
            snackbarHost = { MeshSnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
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
                    SettingsSectionHeader(stringResource(R.string.settings_section_appearance))
                }
                item {
                    GlassPanel(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                            Spacer(Modifier.height(spacing.sm))
                            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                                ThemeMode.entries.forEach { mode ->
                                    val label = when (mode) {
                                        ThemeMode.SYSTEM -> stringResource(R.string.settings_theme_system)
                                        ThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
                                        ThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
                                    }
                                    FilterChip(
                                        selected = uiState.themeMode == mode,
                                        onClick = { viewModel.onEvent(SettingsUiEvent.ChangeTheme(mode)) },
                                        label = { Text(label) }
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Notifications & Chat Heads ─────────────────────────────
                item { SettingsSectionHeader("Notifications & Chat Heads") }
                item {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    GlassPanel(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Enable Chat Heads Overlay", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                                    Text("Facebook Messenger-style floating avatar overlays over other apps", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = uiState.chatHeadsEnabled,
                                    onCheckedChange = { viewModel.onEvent(SettingsUiEvent.ToggleChatHeads(it)) }
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Enable Android Bubble Chat", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                                    Text("Show system notification bubbles on Android 11+", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = uiState.bubblesEnabled,
                                    onCheckedChange = { viewModel.onEvent(SettingsUiEvent.ToggleBubbles(it)) }
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Enable Floating Chat / Direct Reply", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                                    Text("Allow inline replies directly from notification banner", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = uiState.floatingChatEnabled,
                                    onCheckedChange = { viewModel.onEvent(SettingsUiEvent.ToggleFloatingChat(it)) }
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Notification Sound", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                                    Text("Play alert tone for incoming messages", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = uiState.soundEnabled,
                                    onCheckedChange = { viewModel.onEvent(SettingsUiEvent.ToggleSound(it)) }
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Vibration Alert", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                                    Text("Vibrate device on incoming message", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = uiState.vibrationEnabled,
                                    onCheckedChange = { viewModel.onEvent(SettingsUiEvent.ToggleVibration(it)) }
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Popup Preview", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                                    Text("Show heads-up preview banner on screen", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = uiState.popupPreviewEnabled,
                                    onCheckedChange = { viewModel.onEvent(SettingsUiEvent.TogglePopupPreview(it)) }
                                )
                            }

                            Spacer(Modifier.height(4.dp))

                            MeshOutlinedButton(
                                text = "Grant Display Over Other Apps Permission",
                                onClick = {
                                    try {
                                        val overlayIntent = android.content.Intent(
                                            android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            android.net.Uri.parse("package:${context.packageName}")
                                        )
                                        context.startActivity(overlayIntent)
                                    } catch (e: Exception) {
                                        val osIntent = android.content.Intent(android.provider.Settings.ACTION_SETTINGS)
                                        context.startActivity(osIntent)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )

                            MeshOutlinedButton(
                                text = "OS Notification & Bubble Settings",
                                onClick = {
                                    try {
                                        val osIntent = android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                            putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                                        }
                                        context.startActivity(osIntent)
                                    } catch (e: Exception) {
                                        // Fallback to system settings
                                        val osIntent = android.content.Intent(android.provider.Settings.ACTION_SETTINGS)
                                        context.startActivity(osIntent)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // ── Language ──────────────────────────────────────────────────
                item { SettingsSectionHeader(stringResource(R.string.settings_section_language)) }
                item {
                    GlassPanel(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text(stringResource(R.string.settings_language), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                            Spacer(Modifier.height(spacing.sm))
                            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                                listOf(
                                    "system" to stringResource(R.string.settings_language_system),
                                    "en" to stringResource(R.string.settings_language_english),
                                    "bn" to stringResource(R.string.settings_language_bangla)
                                ).forEach { (code, label) ->
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
                item { SettingsSectionHeader(stringResource(R.string.settings_section_privacy)) }
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
                                text = stringResource(R.string.settings_delete_messages),
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
                                text = stringResource(R.string.settings_remove_devices),
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
                                text = stringResource(R.string.settings_rotate_keys),
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
                                text = stringResource(R.string.settings_wipe_data),
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

                // ── Developer ─────────────────────────────────────────────────────
                item { SettingsSectionHeader(stringResource(R.string.settings_developer)) }
                item {
                    GlassPanel(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(stringResource(R.string.settings_debug_mode), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                                    Text(stringResource(R.string.settings_debug_mode_desc), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = uiState.debugModeEnabled,
                                    onCheckedChange = { viewModel.onEvent(SettingsUiEvent.ToggleDebugMode(it)) }
                                )
                            }
                            Spacer(Modifier.height(spacing.md))
                            MeshOutlinedButton(
                                text = stringResource(R.string.settings_clear_logs),
                                onClick = { viewModel.onEvent(SettingsUiEvent.ClearLogs) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // ── About ─────────────────────────────────────────────────────
                item { SettingsSectionHeader(stringResource(R.string.settings_section_about)) }
                item {
                    GlassPanel(modifier = Modifier.fillMaxWidth(), variant = GlassPanelVariant.DEFAULT) {
                        Column {
                            SettingsInfoRow(stringResource(R.string.settings_about_version), uiState.appVersion)
                            SettingsInfoRow(stringResource(R.string.settings_about_build), "Debug")
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
        MeshConfirmationDialog(
            title = confirmTitle,
            message = confirmMessage,
            confirmText = "Proceed",
            cancelText = "Cancel",
            onConfirm = {
                pendingEvent?.let { viewModel.onEvent(it) }
                showConfirmDialog = false
            },
            onCancel = {
                showConfirmDialog = false
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
