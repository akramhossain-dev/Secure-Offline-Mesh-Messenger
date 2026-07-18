/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.storage

import kotlinx.coroutines.flow.Flow

/**
 * Interface contract for local persistent key-value configuration storage (DataStore).
 */
interface StorageManager {
    /** Persist a string setting. */
    suspend fun saveString(key: String, value: String)

    /** Retrieve a string setting stream. */
    fun getString(key: String, defaultValue: String): Flow<String>

    /** Persist a boolean setting. */
    suspend fun saveBoolean(key: String, value: Boolean)

    /** Retrieve a boolean setting stream. */
    fun getBoolean(key: String, defaultValue: Boolean): Flow<Boolean>

    /** Erases all key-value configurations. */
    suspend fun clear()
}
