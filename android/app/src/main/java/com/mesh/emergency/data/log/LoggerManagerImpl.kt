/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.log

import com.mesh.emergency.core.common.result.Result
import com.mesh.emergency.core.log.LogCategory
import com.mesh.emergency.core.log.LogLevel
import com.mesh.emergency.core.log.LoggerManager
import com.mesh.emergency.data.local.LocalDataSource
import com.mesh.emergency.data.local.entity.LogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.PrintWriter
import java.io.StringWriter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [LoggerManager] routing logs into Room database tables.
 */
@Singleton
class LoggerManagerImpl @Inject constructor(
    private val localDataSource: LocalDataSource
) : LoggerManager {

    private val logScope = CoroutineScope(Dispatchers.IO)
    private var debugEnabled = true

    override val isDebugMode: Boolean
        get() = debugEnabled

    override fun log(
        level: LogLevel,
        category: LogCategory,
        message: String,
        moduleName: String,
        exception: Throwable?
    ) {
        if (level == LogLevel.DEBUG && !debugEnabled) return

        val stackTrace = exception?.let {
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            it.printStackTrace(pw)
            sw.toString()
        }

        val log = LogEntity(
            entityId = UUID.randomUUID().toString(),
            level = level.name,
            category = category.name,
            message = message,
            timestamp = System.currentTimeMillis(),
            deviceId = "local_device",
            moduleName = moduleName,
            stackTrace = stackTrace
        )

        // Asynchronously persist logs to database to avoid locking main threads
        logScope.launch {
            try {
                localDataSource.insertLog(log)
            } catch (e: Exception) {
                // Fail-safe print to stderr to prevent crash loops
                System.err.println("Logger persist failure: ${e.message}")
            }
        }

        println("[LOG] [${level.name}] [${category.name}] Module: $moduleName - $message")
    }

    override fun getLogsFlow(): Flow<Result<List<LogEntity>>> {
        return localDataSource.getLogs().map { Result.Success(it) }
    }

    override suspend fun clearLogs(): Result<Unit> {
        return try {
            localDataSource.clearLogs()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override fun exportLogsToString(): Flow<Result<String>> = flow {
        emit(Result.Loading)
        try {
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            // Query current list synchronously for export sweeps
            localDataSource.getLogs().map { list ->
                list.forEach { log ->
                    pw.println("[${log.level}] [${log.category}] Time: ${log.timestamp} Module: ${log.moduleName} - ${log.message}")
                    log.stackTrace?.let { pw.println(it) }
                }
            }.map { }.toString()

            pw.flush()
            emit(Result.Success(sw.toString()))
        } catch (e: Exception) {
            emit(Result.Error(e))
        }
    }

    override fun toggleDebugMode(enabled: Boolean) {
        debugEnabled = enabled
    }
}
