/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.presentation

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

/**
 * Stable UI model wrappers to prevent unnecessary Compose recompositions.
 *
 * Annotating with @Stable or @Immutable tells the Compose compiler that
 * these types' equality is reliable, enabling smart recomposition skipping.
 *
 * Use these wrappers for data passed to frequently-recomposed composables
 * like list items in LazyColumn.
 */

// ─────────────────────────────────────────────────────────────────────────────
// Immutable list wrapper — prevents recomposition when list reference changes
// but content is identical.
// ─────────────────────────────────────────────────────────────────────────────

@Immutable
data class ImmutableList<T>(val items: List<T> = emptyList()) {
    val size: Int get() = items.size
    fun isEmpty(): Boolean = items.isEmpty()
    operator fun get(index: Int): T = items[index]
}

fun <T> List<T>.toImmutable(): ImmutableList<T> = ImmutableList(this)

// ─────────────────────────────────────────────────────────────────────────────
// Stable node model for LazyColumn keys
// ─────────────────────────────────────────────────────────────────────────────

@Stable
data class StableNodeItem(
    val id: String,
    val deviceId: String,
    val type: String,
    val status: String,
    val rssi: Int,
    val batteryLevel: Int,
    val lastSeen: Long,
    val relayCapability: Boolean
)

// ─────────────────────────────────────────────────────────────────────────────
// Stable resource model for filtered list rendering
// ─────────────────────────────────────────────────────────────────────────────

@Stable
data class StableResourceItem(
    val id: String,
    val name: String,
    val type: String,
    val quantity: Int,
    val status: String,
    val description: String
)

// ─────────────────────────────────────────────────────────────────────────────
// Stable activity item for the home screen feed
// ─────────────────────────────────────────────────────────────────────────────

@Stable
data class StableActivityItem(
    val id: String,
    val title: String,
    val description: String,
    val typeLabel: String,
    val timestamp: Long
)
