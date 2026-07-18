/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.designsystem.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mesh.emergency.core.designsystem.theme.MeshThemeTokens

// ─────────────────────────────────────────────────────────────────────────────
// UiState — Sealed class for composable screen state management
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Represents the canonical UI states for any data-driven screen.
 * Use with [UiStateBox] to render the correct composable per state.
 */
sealed class UiState<out T> {
    data object Loading : UiState<Nothing>()
    data object Empty   : UiState<Nothing>()
    data class  Success<T>(val data: T) : UiState<T>()
    data class  Error(val message: String, val retryable: Boolean = true) : UiState<Nothing>()
    data object Offline : UiState<Nothing>()
}

// ─────────────────────────────────────────────────────────────────────────────
// UiStateBox — Animated container rendering the correct state composable
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Renders the appropriate UI for each [UiState] with smooth fade transitions.
 *
 * @param state The current [UiState] to render.
 * @param onRetry Called when the user taps the retry action in [UiState.Error].
 * @param content Slot composable rendered when state is [UiState.Success].
 */
@Composable
fun <T> UiStateBox(
    state: UiState<T>,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    content: @Composable (T) -> Unit,
) {
    AnimatedContent(
        targetState = state,
        transitionSpec = {
            fadeIn(animationSpec = tween(200)) togetherWith
            fadeOut(animationSpec = tween(150))
        },
        label = "UiStateTransition",
        modifier = modifier
    ) { targetState ->
        when (targetState) {
            is UiState.Loading -> UiLoadingState()
            is UiState.Empty   -> UiEmptyState()
            is UiState.Offline -> UiOfflineState()
            is UiState.Error   -> UiErrorState(
                message = targetState.message,
                retryable = targetState.retryable,
                onRetry = onRetry
            )
            is UiState.Success -> content(targetState.data)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Individual State Composables
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun UiLoadingState(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxWidth().height(160.dp)
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(36.dp)
        )
    }
}

@Composable
private fun UiEmptyState(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxWidth().height(200.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Nothing here yet",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun UiOfflineState(modifier: Modifier = Modifier) {
    val semanticColors = MeshThemeTokens.semanticColors
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxWidth().height(200.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "No mesh node connected",
                style = MaterialTheme.typography.bodyLarge,
                color = semanticColors.offline,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Connect via Bluetooth or LoRa to communicate",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun UiErrorState(
    message: String,
    retryable: Boolean,
    onRetry: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val semanticColors = MeshThemeTokens.semanticColors
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxWidth().height(200.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Something went wrong",
                style = MaterialTheme.typography.bodyLarge,
                color = semanticColors.emergency,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (retryable && onRetry != null) {
                Spacer(Modifier.height(12.dp))
                MeshTextButton(text = "Retry", onClick = onRetry)
            }
        }
    }
}
