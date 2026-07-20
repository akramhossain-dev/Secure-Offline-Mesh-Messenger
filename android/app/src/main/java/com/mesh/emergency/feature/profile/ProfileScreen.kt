/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.feature.profile

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import com.mesh.emergency.core.discovery.qr.QRHandshakeData
import com.mesh.emergency.core.discovery.qr.QRHandshakeManager
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mesh.emergency.R
import com.mesh.emergency.core.designsystem.component.*
import com.mesh.emergency.core.designsystem.theme.MeshThemeTokens

/**
 * Screen rendering the user's local cryptographic profile, identity QR code, and nickname controls.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val spacing = MeshThemeTokens.spacing
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var showSaveSuccess by remember { androidx.compose.runtime.mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ProfileUiEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                ProfileUiEffect.SaveSuccess -> {
                    showSaveSuccess = true
                }
            }
        }
    }

    if (showSaveSuccess) {
        MeshSuccessDialog(
            title = "Profile Saved",
            message = "Your profile nickname has been updated successfully.",
            onConfirm = {
                showSaveSuccess = false
                onBack()
            }
        )
    }

    AuroraBackdrop(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.profile_title), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Default.ArrowBack,
                                contentDescription = stringResource(R.string.action_back)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { paddingValues ->
            if (uiState.isLoading) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    ShimmerPlaceholder(modifier = Modifier.size(240.dp))
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = spacing.lg, end = spacing.lg,
                        top = paddingValues.calculateTopPadding() + spacing.sm,
                        bottom = spacing.lg
                    ),
                    verticalArrangement = Arrangement.spacedBy(spacing.md),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize()
                ) {

                    // ── 1. Profile Photo Header ────────────────────────────────
                    item {
                        val initials = uiState.userModel?.username?.take(2)?.uppercase() ?: "ME"
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        ) {
                            Text(
                                text = initials,
                                color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    // ── 2. Nickname Configuration Panel ─────────────────────────
                    item {
                        GlassPanel(modifier = Modifier.fillMaxWidth()) {
                            Column {
                                Text(
                                    stringResource(R.string.profile_nickname_label),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.height(spacing.sm))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = uiState.usernameInput,
                                        onValueChange = { viewModel.onUsernameChanged(it) },
                                        label = { Text(stringResource(R.string.profile_nickname_hint)) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(Modifier.width(spacing.sm))
                                    IconButton(
                                        onClick = { viewModel.saveProfile() },
                                        modifier = Modifier
                                            .size(52.dp)
                                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
                                    ) {
                                        Icon(
                                            Icons.Default.Save,
                                            contentDescription = stringResource(R.string.action_save),
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ── 3. Profile Identity Handshake QR Card ───────────────────
                    item {
                        val handshakePayload = remember(uiState.userModel) {
                            uiState.userModel?.let {
                                QRHandshakeManager.generatePayload(
                                    QRHandshakeData(
                                        version = 1,
                                        deviceId = it.id,
                                        userId = it.id,
                                        username = it.username,    // real display name
                                        deviceType = "SMARTPHONE",
                                        publicKeyRef = it.publicKey,
                                        timestamp = System.currentTimeMillis(),
                                        bleAddress = viewModel.localBleAddress
                                    )
                                )
                            } ?: ""
                        }

                        GlassPanel(
                            modifier = Modifier.fillMaxWidth(),
                            variant = GlassPanelVariant.DEFAULT
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    stringResource(R.string.profile_qr_title),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    stringResource(R.string.profile_qr_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = spacing.xs)
                                )
                                Spacer(Modifier.height(spacing.sm))

                                QrCodeImage(
                                    payload = handshakePayload,
                                    modifier = Modifier
                                        .size(200.dp)
                                        .border(2.dp, MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
                                )
                            }
                        }
                    }

                    // ── 4. Cryptographic Key Details ──────────────────────────────
                    item {
                        val fingerprint = uiState.userModel?.id ?: ""
                        val key = uiState.userModel?.publicKey ?: ""
                        val fingerprintCopied = stringResource(R.string.profile_copied_fingerprint)
                        val keyCopied = stringResource(R.string.profile_copied_key)

                        GlassPanel(modifier = Modifier.fillMaxWidth()) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(stringResource(R.string.profile_fingerprint_label), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(fingerprint.take(24) + "...", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                                    }
                                    IconButton(onClick = {
                                        clipboardManager.setText(AnnotatedString(fingerprint))
                                        Toast.makeText(context, fingerprintCopied, Toast.LENGTH_SHORT).show()
                                    }) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Fingerprint", modifier = Modifier.size(18.dp))
                                    }
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(stringResource(R.string.profile_key_label), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(key.take(24) + "...", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                                    }
                                    IconButton(onClick = {
                                        clipboardManager.setText(AnnotatedString(key))
                                        Toast.makeText(context, keyCopied, Toast.LENGTH_SHORT).show()
                                    }) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Public Key", modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Lightweight, high-performance Canvas-based QR Code generator.
 * Creates version-compliant finder patterns and translates the payload string deterministically into a 2D matrix.
 */
@Composable
fun QrCodeImage(
    payload: String,
    modifier: Modifier = Modifier
) {
    val bitMatrix = remember(payload) {
        try {
            if (payload.isNotEmpty()) {
                QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, 512, 512)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    Canvas(modifier = modifier) {
        if (bitMatrix != null) {
            val width = bitMatrix.width
            val height = bitMatrix.height
            val cellSizeX = size.width / width
            val cellSizeY = size.height / height

            // Draw solid white background
            drawRect(color = Color.White)

            // Draw black modules
            for (y in 0 until height) {
                for (x in 0 until width) {
                    if (bitMatrix.get(x, y)) {
                        drawRect(
                            color = Color.Black,
                            topLeft = Offset(x * cellSizeX, y * cellSizeY),
                            size = Size(cellSizeX, cellSizeY)
                        )
                    }
                }
            }
        } else {
            // Draw loading or fallback indicator
            drawRect(color = Color.LightGray)
        }
    }
}
