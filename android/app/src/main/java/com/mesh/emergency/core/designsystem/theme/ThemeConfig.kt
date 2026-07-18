/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.designsystem.theme

/**
 * Enumerates the supported theme modes for the Offline Emergency Mesh application.
 *
 * The selected mode is persisted in DataStore via [com.mesh.emergency.core.common.constants.AppConstants.PREF_KEY_THEME_MODE]
 * and applied at application startup through [MeshTheme].
 *
 * Phase A2: A ThemeViewModel will read this from DataStore and expose it as
 * a StateFlow to the root composable in MainActivity.
 */
enum class ThemeMode {

    /**
     * Follows the system-level dark/light setting (Android 10+).
     * This is the default and recommended option.
     */
    SYSTEM,

    /**
     * Always uses the light color scheme regardless of system setting.
     */
    LIGHT,

    /**
     * Always uses the dark color scheme regardless of system setting.
     */
    DARK;

    companion object {

        /** The default theme mode applied on first launch. */
        val DEFAULT: ThemeMode = SYSTEM

        /**
         * Converts a persisted [String] value back to a [ThemeMode].
         * Returns [DEFAULT] for unrecognized values.
         */
        fun fromString(value: String): ThemeMode =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: DEFAULT
    }
}
