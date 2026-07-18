/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.log

import com.mesh.emergency.core.common.result.Result
import com.mesh.emergency.data.local.entity.LogEntity
import kotlinx.coroutines.flow.Flow

/**
 * Interface contract coordinating diagnostic logging systems.
 */
interface LoggerManager {
    /** Exposes debug status. */
    val isDebugMode: Boolean

    /** Logs a diagnostic event. */
    fun log(
        level: LogLevel,
        category: LogCategory,
        message: String,
        moduleName: String,
        exception: Throwable? = null
    )

    /** Streams recent cached logs from the local database. */
    fun getLogsFlow(): Flow<Result<List<LogEntity>>>

    /** Clears all logs. */
    suspend fun clearLogs(): Result<Unit>

    /** Exports recent logs to formatted strings. */
    fun exportLogsToString(): Flow<Result<String>>

    /** Enables or disables debug level parameters filtering. */
    fun toggleDebugMode(enabled: Boolean)
}

/**
 * Diagnostic log levels.
 */
enum class LogLevel {
    DEBUG,
    INFO,
    WARNING,
    ERROR,
    CRITICAL
}

/**
 * Diagnostic log categories.
 */
enum class LogCategory {
    SYSTEM,
    NETWORK,
    BLUETOOTH,
    LORA,
    SECURITY,
    BATTERY,
    LOCATION
}
