/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.di

import com.mesh.emergency.core.security.CryptographyEngine
import com.mesh.emergency.core.security.CryptographyEngineImpl
import com.mesh.emergency.core.security.KeyManager
import com.mesh.emergency.core.security.KeyStorage
import com.mesh.emergency.data.security.KeyManagerImpl
import com.mesh.emergency.data.security.KeyStorageImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module binding local security, hashing, and encryption managers.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SecurityModule {

    /** Binds [CryptographyEngineImpl] to the [CryptographyEngine] interface. */
    @Binds
    @Singleton
    abstract fun bindCryptographyEngine(impl: CryptographyEngineImpl): CryptographyEngine

    /** Binds [KeyStorageImpl] to the [KeyStorage] interface. */
    @Binds
    @Singleton
    abstract fun bindKeyStorage(impl: KeyStorageImpl): KeyStorage

    /** Binds [KeyManagerImpl] to the [KeyManager] interface. */
    @Binds
    @Singleton
    abstract fun bindKeyManager(impl: KeyManagerImpl): KeyManager
}
