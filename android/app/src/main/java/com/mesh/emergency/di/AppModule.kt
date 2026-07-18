/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.mesh.emergency.core.common.constants.AppConstants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// Extension property to create the DataStore instance
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = AppConstants.PREFS_NAME
)

/**
 * Hilt module providing application-scoped infrastructure dependencies.
 *
 * Phase A1: Provides DataStore for preferences persistence (theme, language).
 * Phase A2+: Additional modules will be created:
 *   - DatabaseModule (Room)
 *   - CommunicationModule (BLE, LoRa)
 *   - CryptoModule (Encryption)
 *   - RepositoryModule (all repository bindings)
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Provides the application-scoped [DataStore] instance.
     * Used by [LocaleManager] for language preference and
     * ThemeManager for theme preference (Phase A2).
     */
    @Provides
    @Singleton
    fun provideDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.dataStore
}
