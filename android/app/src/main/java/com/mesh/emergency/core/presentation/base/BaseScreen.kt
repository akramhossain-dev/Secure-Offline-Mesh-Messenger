/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.presentation.base

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * A standardized scaffolding screen wrapper to build Compose screens.
 * Automatically displays progress indicators and handles snackbar notifications.
 *
 * @param isLoading Triggers a centered loading indicator when true.
 * @param errorMessage Displays a snackbar notification with this message when non-null.
 * @param onDismissError Callback triggered once the error snackbar has been displayed/dismissed.
 * @param topBar Top app bar composable block.
 * @param bottomBar Bottom navigation bar composable block.
 * @param content Inside content block receiving layout modifiers.
 */
@Composable
fun BaseScreen(
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onDismissError: () -> Unit = {},
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (modifier: Modifier) -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    errorMessage?.let { message ->
        LaunchedEffect(message) {
            snackbarHostState.showSnackbar(message)
            onDismissError()
        }
    }

    Scaffold(
        topBar = topBar,
        bottomBar = bottomBar,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            content(Modifier.fillMaxSize())

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}
