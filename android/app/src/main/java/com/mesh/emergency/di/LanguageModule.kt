/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.di

import com.mesh.emergency.core.localization.LanguageManager
import com.mesh.emergency.data.localization.LanguageManagerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module binding local localization and language managers.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class LanguageModule {

    /** Binds [LanguageManagerImpl] to the [LanguageManager] interface. */
    @Binds
    @Singleton
    abstract fun bindLanguageManager(impl: LanguageManagerImpl): LanguageManager
}
