/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.utils.capability

/**
 * Interface contract for auditing hardware features availability.
 */
interface DeviceCapabilityManager {
    /** Returns true if Bluetooth LE is supported on this hardware. */
    fun isBluetoothSupported(): Boolean

    /** Returns true if GPS location features are supported. */
    fun isLocationSupported(): Boolean

    /** Returns true if Microphone recording is supported. */
    fun isMicrophoneSupported(): Boolean

    /** Returns true if Notification alerts are supported. */
    fun isNotificationsSupported(): Boolean

    /** Returns true if GPS locations receiver is powered on. */
    fun isGpsEnabled(): Boolean
}
