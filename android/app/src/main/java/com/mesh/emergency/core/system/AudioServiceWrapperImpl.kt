/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.system

import android.content.Context
import android.media.AudioManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [AudioServiceWrapper] wrapping system [AudioManager].
 */
@Singleton
class AudioServiceWrapperImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AudioServiceWrapper {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    override fun setMicrophoneMute(state: Boolean) {
        audioManager?.isMicrophoneMute = state
    }

    override fun isMicrophoneMuted(): Boolean {
        return audioManager?.isMicrophoneMute == true
    }
}
