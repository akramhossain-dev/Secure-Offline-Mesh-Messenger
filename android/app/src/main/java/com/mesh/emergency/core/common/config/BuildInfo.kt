/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.common.config

import com.mesh.emergency.BuildConfig

/**
 * Runtime build information wrapper.
 *
 * Provides a single typed interface to all [BuildConfig] values,
 * making it easy to mock in tests and avoiding direct BuildConfig
 * references scattered through the codebase.
 */
object BuildInfo {

    /** Application version name (e.g. "0.1.0"). */
    val VERSION_NAME: String = BuildConfig.VERSION_NAME

    /** Application version code (integer build number). */
    val VERSION_CODE: Int = BuildConfig.VERSION_CODE

    /** Application package/application ID. */
    val APPLICATION_ID: String = BuildConfig.APPLICATION_ID

    /** `true` if this is a debug build. */
    val IS_DEBUG: Boolean = BuildConfig.DEBUG

    /** `true` if this is a release build. */
    val IS_RELEASE: Boolean = !BuildConfig.DEBUG

    /** Build environment label: "debug" or "release". */
    val ENVIRONMENT: String = BuildConfig.ENVIRONMENT

    /** Whether verbose logging is enabled (set via buildConfigField). */
    val ENABLE_LOGGING: Boolean = BuildConfig.ENABLE_LOGGING

    // ─────────────────────────────────────────────────────────────────────────
    // Derived helpers
    // ─────────────────────────────────────────────────────────────────────────

    /** Human-readable version string for display in About screen. */
    val displayVersion: String
        get() = "v$VERSION_NAME ($VERSION_CODE)"

    /** Full build identifier string. */
    val buildIdentifier: String
        get() = "$APPLICATION_ID@$VERSION_NAME-$ENVIRONMENT"

    override fun toString(): String =
        "BuildInfo[$buildIdentifier, debug=$IS_DEBUG]"
}
