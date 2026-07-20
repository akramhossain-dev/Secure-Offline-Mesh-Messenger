/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.feature.message.presentation

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.mesh.emergency.R
import timber.log.Timber
import java.util.concurrent.Executors
import com.mesh.emergency.core.common.extensions.hasPermission
import com.mesh.emergency.core.designsystem.component.*

/**
 * QR Scanner screen — securely pairs with mesh contacts by exchanging cryptographic keys.
 *
 * Camera permission handling:
 * 1. Not granted, first time → request permission
 * 2. Not granted, user can be asked again → show rationale dialog, then request
 * 3. Permanently denied → show "Open Settings" UI
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrPairScreen(
    onBack: () -> Unit,
    viewModel: QrPairViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = LocalActivity.current

    // Permission state tracking
    var hasCameraPermission by remember {
        mutableStateOf(context.hasPermission(Manifest.permission.CAMERA))
    }
    var alreadyRequested by rememberSaveable { mutableStateOf(false) }
    var showRationaleDialog by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }

    var successMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val isPermanentlyDenied = alreadyRequested &&
        !hasCameraPermission &&
        activity != null &&
        !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        alreadyRequested = true
        hasCameraPermission = granted
        if (!granted && activity != null &&
            ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
        ) {
            showRationaleDialog = true
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is QrPairUiEffect.Success -> {
                    successMessage = effect.message
                }
                is QrPairUiEffect.Error -> {
                    errorMessage = effect.message
                    isProcessing = false
                }
            }
        }
    }

    if (successMessage != null) {
        MeshSuccessDialog(
            title = "Pairing Successful",
            message = successMessage!!,
            onConfirm = {
                successMessage = null
                onBack()
            }
        )
    }

    if (errorMessage != null) {
        MeshErrorDialog(
            title = "Pairing Failed",
            message = errorMessage!!,
            onClose = {
                errorMessage = null
            }
        )
    }

    if (isProcessing) {
        MeshLoadingDialog(
            title = "Connecting to Node...",
            message = "Establishing secure cryptographic connection..."
        )
    }

    // Rationale dialog — shown when user denied once and can still be asked
    if (showRationaleDialog) {
        AlertDialog(
            onDismissRequest = { showRationaleDialog = false },
            icon = {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    stringResource(R.string.permission_camera_rationale_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    stringResource(R.string.permission_camera_rationale_desc),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                MeshButton(
                    text = stringResource(R.string.action_grant_permission),
                    onClick = {
                        showRationaleDialog = false
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { showRationaleDialog = false; onBack() }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    AuroraBackdrop(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            stringResource(R.string.qr_pair_title),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    },
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                when {
                    isPermanentlyDenied -> {
                        // ── Permanently Denied State ─────────────────────────
                        CameraPermissionPermanentlyDenied(
                            onOpenSettings = {
                                context.startActivity(
                                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    }
                                )
                            }
                        )
                    }

                    !hasCameraPermission -> {
                        // ── Permission Not Granted — Request State ───────────
                        CameraPermissionRequest(
                            onRequest = {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        )
                    }

                    else -> {
                        // ── Camera Granted — QR Scanner UI ───────────────────
                        QrScannerContent(
                            isProcessing = isProcessing,
                            onQrCodeDetected = { payload ->
                                if (!isProcessing) {
                                    isProcessing = true
                                    viewModel.processHandshakePayload(payload)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Camera Permission — Request State
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CameraPermissionRequest(onRequest: () -> Unit) {
    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        variant = GlassPanelVariant.DEFAULT
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Default.CameraAlt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.permission_camera_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.permission_camera_rationale_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            MeshButton(
                text = stringResource(R.string.action_grant_permission),
                onClick = onRequest,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Camera Permission — Permanently Denied State
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CameraPermissionPermanentlyDenied(onOpenSettings: () -> Unit) {
    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        variant = GlassPanelVariant.WARNING
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Default.Settings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.permission_permanently_denied_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.permission_permanently_denied_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onOpenSettings,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Settings, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.permission_open_settings))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// QR Scanner Content (camera permission granted)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun QrScannerContent(
    isProcessing: Boolean,
    onQrCodeDetected: (String) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "LaserScan")
    val laserOffsetY by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LaserOffset"
    )

    // ── 1. Camera Viewfinder Overlay ────────────────────────────────────
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(280.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Black)
            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(24.dp))
    ) {
        if (!isProcessing) {
            CameraPreview(onQrCodeDetected = onQrCodeDetected)
        }
        
        // Visual scanner guides with animated laser line and glowing neon corners
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            if (!isProcessing) {
                // Neon corners
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 3.dp.toPx()
                    val len = 24.dp.toPx()
                    val r = 12.dp.toPx()
                    val primaryColor = Color(0xFF5ADFF0) // Electric teal neon
                    
                    // Top Left Corner
                    drawArc(
                        color = primaryColor,
                        startAngle = 180f,
                        sweepAngle = 90f,
                        useCenter = false,
                        style = Stroke(width = stroke)
                    )
                    drawLine(color = primaryColor, start = Offset(0f, r), end = Offset(0f, len), strokeWidth = stroke)
                    drawLine(color = primaryColor, start = Offset(r, 0f), end = Offset(len, 0f), strokeWidth = stroke)

                    // Top Right Corner
                    drawArc(
                        color = primaryColor,
                        startAngle = 270f,
                        sweepAngle = 90f,
                        useCenter = false,
                        style = Stroke(width = stroke)
                    )
                    drawLine(color = primaryColor, start = Offset(size.width, r), end = Offset(size.width, len), strokeWidth = stroke)
                    drawLine(color = primaryColor, start = Offset(size.width - r, 0f), end = Offset(size.width - len, 0f), strokeWidth = stroke)

                    // Bottom Left Corner
                    drawArc(
                        color = primaryColor,
                        startAngle = 90f,
                        sweepAngle = 90f,
                        useCenter = false,
                        style = Stroke(width = stroke)
                    )
                    drawLine(color = primaryColor, start = Offset(0f, size.height - r), end = Offset(0f, size.height - len), strokeWidth = stroke)
                    drawLine(color = primaryColor, start = Offset(r, size.height), end = Offset(len, size.height), strokeWidth = stroke)

                    // Bottom Right Corner
                    drawArc(
                        color = primaryColor,
                        startAngle = 0f,
                        sweepAngle = 90f,
                        useCenter = false,
                        style = Stroke(width = stroke)
                    )
                    drawLine(color = primaryColor, start = Offset(size.width, size.height - r), end = Offset(size.width, size.height - len), strokeWidth = stroke)
                    drawLine(color = primaryColor, start = Offset(size.width - r, size.height), end = Offset(size.width - len, size.height), strokeWidth = stroke)
                }

                // Laser scan line
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .align(Alignment.TopCenter)
                        .offset(y = 200.dp * laserOffsetY)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color(0xFF5ADFF0).copy(alpha = 0.8f),
                                    Color(0xFF5ADFF0),
                                    Color(0xFF5ADFF0).copy(alpha = 0.8f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }
        }

        if (isProcessing) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(Modifier.height(140.dp))
                Text(
                    stringResource(R.string.qr_pair_scan_label),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(32.dp))

    // ── 2. Information Context Card ──────────────────────────────────────────
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.qr_pair_info_title),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.qr_pair_info_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun CameraPreview(
    onQrCodeDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val executor = remember { Executors.newSingleThreadExecutor() }

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    val barcodeScanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
        )
    }

    val hasDetected = remember { java.util.concurrent.atomic.AtomicBoolean(false) }

    val stableOnQrCodeDetected = remember(onQrCodeDetected) {
        { payload: String ->
            if (hasDetected.compareAndSet(false, true)) {
                onQrCodeDetected(payload)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            executor.shutdown()
            barcodeScanner.close()
        }
    }

    LaunchedEffect(lifecycleOwner) {
        val cameraProvider = cameraProviderFuture.get()
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { analysis ->
                analysis.setAnalyzer(executor) { imageProxy ->
                    processImageProxy(barcodeScanner, imageProxy, stableOnQrCodeDetected)
                }
            }

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageAnalysis
            )
        } catch (exc: Exception) {
            Timber.e(exc, "Use case binding failed")
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize()
    )
}

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
private fun processImageProxy(
    barcodeScanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    imageProxy: ImageProxy,
    onQrCodeDetected: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        barcodeScanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    barcode.rawValue?.let { value ->
                        onQrCodeDetected(value)
                    }
                }
            }
            .addOnFailureListener {
                Timber.e(it, "Barcode scanning failed")
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}
