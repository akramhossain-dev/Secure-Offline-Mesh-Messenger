/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.feature.dashboard

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mesh.emergency.R
import com.mesh.emergency.core.designsystem.component.AuroraBackdrop
import com.mesh.emergency.core.designsystem.component.ScanningRipple

/**
 * Splash screen using Aurora backdrop with logo animation, ripple effect,
 * and loading indicator. Navigates to Home after init completes.
 */
@Composable
fun SplashScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                SplashUiEffect.NavigateToHome -> onNavigateToHome()
                SplashUiEffect.NavigateToOnboarding -> onNavigateToOnboarding()
            }
        }
    }

    // ── Entrance animation ────────────────────────────────────────────────────
    val logoAlpha = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.7f) }

    LaunchedEffect(Unit) {
        logoAlpha.animateTo(1f, tween(600, easing = EaseOutCubic))
    }
    LaunchedEffect(Unit) {
        logoScale.animateTo(1f, tween(600, easing = EaseOutCubic))
    }

    AuroraBackdrop(modifier = Modifier.fillMaxSize()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            // ── Logo mark with scanning ripple ────────────────────────────────
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(140.dp)
                    .alpha(logoAlpha.value)
                    .scale(logoScale.value)
            ) {
                // Expanding ring animation around the logo
                ScanningRipple(
                    color = MaterialTheme.colorScheme.primary,
                    size = 96.dp
                )
                // Shield + mesh network logo (dark circle bg baked in)
                Image(
                    painter = painterResource(id = R.drawable.ic_splash_logo),
                    contentDescription = null,
                    modifier = Modifier.size(96.dp)
                )
            }

            Spacer(Modifier.height(32.dp))

            // ── App name ──────────────────────────────────────────────────────
            Text(
                text = uiState.appName,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.alpha(logoAlpha.value)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Offline Mesh Communication",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.alpha(logoAlpha.value)
            )

            Spacer(Modifier.height(48.dp))

            // ── Loading indicator ─────────────────────────────────────────────
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = uiState.version,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.alpha(logoAlpha.value)
            )
        }
    }
}
