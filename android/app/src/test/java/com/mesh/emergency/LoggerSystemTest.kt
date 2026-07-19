/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency

import com.mesh.emergency.core.log.LogCategory
import com.mesh.emergency.core.log.LogLevel
import com.mesh.emergency.data.local.LocalDataSource
import com.mesh.emergency.data.log.LoggerManagerImpl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.any
import org.mockito.MockitoAnnotations

/**
 * Unit test validating local LoggerManager filtering rules and database cache triggers.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LoggerSystemTest {

    @Mock
    private lateinit var mockLocalDataSource: LocalDataSource

    private lateinit var loggerManager: LoggerManagerImpl

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        loggerManager = LoggerManagerImpl(mockLocalDataSource)
    }

    @Test
    fun testLog_persistsLogWhenDebugModeIsEnabled() = runTest {
        assertTrue(loggerManager.isDebugMode)

        loggerManager.log(
            level = LogLevel.INFO,
            category = LogCategory.SYSTEM,
            message = "App booted successfully",
            moduleName = "MainActivity"
        )

        // Give a short delay to let coroutine execute async database log insert
        Thread.sleep(100)
        verify(mockLocalDataSource).insertLog(anyLogEntity())
    }

    @Test
    fun testLog_filtersOutDebugLogsWhenDebugModeIsDisabled() = runTest {
        loggerManager.toggleDebugMode(false)
        assertFalse(loggerManager.isDebugMode)

        loggerManager.log(
            level = LogLevel.DEBUG,
            category = LogCategory.BLUETOOTH,
            message = "Scanning BLE peripherals",
            moduleName = "BluetoothService"
        )

        Thread.sleep(100)
        verify(mockLocalDataSource, never()).insertLog(anyLogEntity())
    }
}

private fun anyLogEntity(): com.mesh.emergency.data.local.entity.LogEntity {
    org.mockito.Mockito.any(com.mesh.emergency.data.local.entity.LogEntity::class.java)
    return com.mesh.emergency.data.local.entity.LogEntity(
        entityId = "",
        level = "",
        category = "",
        message = "",
        timestamp = 0L,
        deviceId = "",
        moduleName = ""
    )
}
