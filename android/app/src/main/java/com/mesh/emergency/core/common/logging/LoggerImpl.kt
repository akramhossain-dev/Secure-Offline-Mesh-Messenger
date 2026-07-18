/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.common.logging

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Logger implementation that forwards calls to [AppLogger].
 */
@Singleton
class LoggerImpl @Inject constructor() : Logger {

    override fun v(tag: String, message: String) {
        AppLogger.v(tag, message)
    }

    override fun d(tag: String, message: String) {
        AppLogger.d(tag, message)
    }

    override fun i(tag: String, message: String) {
        AppLogger.i(tag, message)
    }

    override fun w(tag: String, message: String) {
        AppLogger.w(tag, message)
    }

    override fun e(tag: String, message: String, throwable: Throwable?) {
        AppLogger.e(tag, message, throwable)
    }

    override fun wtf(tag: String, message: String, throwable: Throwable?) {
        AppLogger.wtf(tag, message, throwable)
    }
}
