/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.common.extensions

// ─────────────────────────────────────────────────────────────────────────────
// String Extension Functions
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Returns `true` if this string is not null and not blank (whitespace-only).
 */
fun String?.isNotNullOrBlank(): Boolean = !this.isNullOrBlank()

/**
 * Truncates this string to [maxLength] characters, appending [suffix] if truncated.
 */
fun String.truncate(maxLength: Int, suffix: String = "..."): String =
    if (length <= maxLength) this else take(maxLength - suffix.length) + suffix

/**
 * Converts a hex string (e.g., BLE UUID bytes) to a byte array.
 * Throws [IllegalArgumentException] if the string is not valid hex.
 */
fun String.hexToByteArray(): ByteArray {
    require(length % 2 == 0) { "Hex string must have even length: $this" }
    return chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}

/**
 * Converts a byte array to a lowercase hex string.
 */
fun ByteArray.toHexString(): String = joinToString("") { "%02x".format(it) }

/**
 * Returns this string with the first character uppercased,
 * preserving the rest of the string as-is.
 */
fun String.capitalizeFirst(): String =
    if (isEmpty()) this else this[0].uppercaseChar() + substring(1)

/**
 * Returns a sanitized version of this string safe for use as a filename.
 * Replaces characters not in [a-zA-Z0-9._-] with underscores.
 */
fun String.toSafeFilename(): String = replace(Regex("[^a-zA-Z0-9._-]"), "_")

/**
 * Returns `true` if this string could be a valid node ID
 * (16 hex characters, no dashes).
 */
fun String.isValidNodeId(): Boolean =
    length == 16 && all { it.isLetterOrDigit() }
