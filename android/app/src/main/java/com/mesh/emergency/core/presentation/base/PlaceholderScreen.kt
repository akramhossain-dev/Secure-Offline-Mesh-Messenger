/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.presentation.base

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.mesh.emergency.core.designsystem.component.MeshCenterAlignedTopAppBar

/**
 * Centered placeholder screen displaying screen names.
 * Ensures the project compiles and can be launched without having completed actual features.
 */
@Composable
fun PlaceholderScreen(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {}
) {
    BaseScreen(
        topBar = {
            MeshCenterAlignedTopAppBar(
                title = title,
                navigationIcon = navigationIcon
            )
        },
        modifier = modifier
    ) { contentModifier ->
        Box(
            contentAlignment = Alignment.Center,
            modifier = contentModifier.fillMaxSize()
        ) {
            Text(
                text = "$title Screen Placeholder",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
