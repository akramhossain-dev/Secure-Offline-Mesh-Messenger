/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.system

/**
 * Interface contract wrapping Android AudioManager calls.
 */
interface AudioServiceWrapper {
    /** Sets microphone mute status state. */
    fun setMicrophoneMute(state: Boolean)

    /** Returns true if microphone is muted. */
    fun isMicrophoneMuted(): Boolean
}
