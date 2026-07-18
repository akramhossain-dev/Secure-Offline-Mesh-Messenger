/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [StorageManager] backing settings persistence in Jetpack DataStore.
 */
@Singleton
class StorageManagerImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : StorageManager {

    override suspend fun saveString(key: String, value: String) {
        dataStore.edit { prefs ->
            prefs[stringPreferencesKey(key)] = value
        }
    }

    override fun getString(key: String, defaultValue: String): Flow<String> {
        return dataStore.data.map { prefs ->
            prefs[stringPreferencesKey(key)] ?: defaultValue
        }
    }

    override suspend fun saveBoolean(key: String, value: Boolean) {
        dataStore.edit { prefs ->
            prefs[booleanPreferencesKey(key)] = value
        }
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Flow<Boolean> {
        return dataStore.data.map { prefs ->
            prefs[booleanPreferencesKey(key)] ?: defaultValue
        }
    }

    override suspend fun clear() {
        dataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
