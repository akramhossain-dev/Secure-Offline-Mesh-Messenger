/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.common.logging

/**
 * Interface contract for logging, decoupling layers from concrete log libraries.
 */
interface Logger {
    /** Log at verbose level. */
    fun v(tag: String, message: String)

    /** Log at debug level. */
    fun d(tag: String, message: String)

    /** Log at informational level. */
    fun i(tag: String, message: String)

    /** Log at warning level. */
    fun w(tag: String, message: String)

    /** Log an error with an optional throwable stack trace. */
    fun e(tag: String, message: String, throwable: Throwable? = null)

    /** Log a critical failure (What a Terrible Failure). */
    fun wtf(tag: String, message: String, throwable: Throwable? = null)
}
