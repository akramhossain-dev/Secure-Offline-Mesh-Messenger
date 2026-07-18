/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.di

import com.mesh.emergency.core.audio.AudioProvider
import com.mesh.emergency.core.audio.VoiceManager
import com.mesh.emergency.data.audio.AudioProviderImpl
import com.mesh.emergency.data.audio.VoiceManagerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module binding local audio recording providers and voice managers.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AudioModule {

    /** Binds [AudioProviderImpl] to the [AudioProvider] interface. */
    @Binds
    @Singleton
    abstract fun bindAudioProvider(impl: AudioProviderImpl): AudioProvider

    /** Binds [VoiceManagerImpl] to the [VoiceManager] interface. */
    @Binds
    @Singleton
    abstract fun bindVoiceManager(impl: VoiceManagerImpl): VoiceManager
}
