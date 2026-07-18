/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.power

/**
 * Application optimization profiles based on battery levels.
 */
enum class PowerSavingMode {
    /** Full background search and sync activities enabled. */
    NORMAL,

    /** Reduced sync frequencies and search delays. */
    SAVING,

    /** Communication prioritizes emergency transmissions only. */
    EMERGENCY
}
