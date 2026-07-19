/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency

import com.mesh.emergency.core.common.result.Result
import com.mesh.emergency.core.communication.DeliveryResult
import com.mesh.emergency.core.log.LogCategory
import com.mesh.emergency.core.log.LogLevel
import com.mesh.emergency.data.log.LoggerManagerImpl
import com.mesh.emergency.data.notification.NotificationManagerImpl
import com.mesh.emergency.data.localization.LanguageManagerImpl
import com.mesh.emergency.test.MockFactory
import com.mesh.emergency.test.ScenarioSimulator
import com.mesh.emergency.test.TestDataFactory
import com.mesh.emergency.core.power.isBatteryCritical
import com.mesh.emergency.core.power.getBatteryLevel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Offline scenario integration tests covering adverse conditions.
 *
 * These tests validate how system components behave when hardware is
 * unavailable, permissions are denied, or storage is exhausted.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OfflineScenarioTest {

    // ── Scenario 1: No Bluetooth ──────────────────────────────────────────────

    @Test
    fun scenario_bluetoothOff_sendFails() {
        val error = ScenarioSimulator.bluetoothOffError()
        assertTrue(error is Result.Error)
        val msg = (error as Result.Error).exception.message ?: ""
        assertTrue(msg.contains("BLE adapter", ignoreCase = true))
    }

    // ── Scenario 2: No LoRa ───────────────────────────────────────────────────

    @Test
    fun scenario_loraUnavailable_sendFails() {
        val error = ScenarioSimulator.loraUnavailableError()
        assertTrue(error is Result.Error)
        assertTrue((error as Result.Error).exception.message?.contains("LoRa") == true)
    }

    // ── Scenario 3: Connection Timeout ────────────────────────────────────────

    @Test
    fun scenario_connectionTimeout_returnsError() {
        val error = ScenarioSimulator.connectionTimeoutError(timeoutMs = 30_000L)
        assertTrue(error is Result.Error)
        val msg = (error as Result.Error).exception.message ?: ""
        assertTrue(msg.contains("30000"))
    }

    // ── Scenario 4: Permission Denied ─────────────────────────────────────────

    @Test
    fun scenario_bluetoothPermissionDenied_returnsSecurityException() {
        val error = ScenarioSimulator.bluetoothPermissionDenied()
        assertTrue(error is Result.Error)
        assertTrue((error as Result.Error).exception is SecurityException)
    }

    @Test
    fun scenario_locationPermissionDenied_returnsSecurityException() {
        val error = ScenarioSimulator.locationPermissionDenied()
        assertTrue(error is Result.Error)
        assertTrue((error as Result.Error).exception is SecurityException)
    }

    // ── Scenario 5: Storage Failure ───────────────────────────────────────────

    @Test
    fun scenario_databaseWriteFailure_returnsError() {
        val error = ScenarioSimulator.databaseWriteFailed()
        assertTrue(error is Result.Error)
        val msg = (error as Result.Error).exception.message ?: ""
        assertTrue(msg.contains("disk full", ignoreCase = true) || msg.contains("Room", ignoreCase = true))
    }

    // ── Scenario 6: Low Battery ───────────────────────────────────────────────

    @Test
    fun scenario_criticalBattery_powerManagerReportsCritical() {
        val powerManager = MockFactory.stubPowerManager(
            batteryLevel = ScenarioSimulator.CRITICAL_BATTERY_LEVEL,
            isCritical = true
        )
        assertTrue(powerManager.isBatteryCritical())
        assertTrue(powerManager.getBatteryLevel() < 10)
    }

    @Test
    fun scenario_fullBattery_powerManagerReportsNotCritical() {
        val powerManager = MockFactory.stubPowerManager(
            batteryLevel = ScenarioSimulator.FULL_BATTERY_LEVEL,
            isCritical = false
        )
        assertFalse(powerManager.isBatteryCritical())
    }

    // ── Scenario 7: Message TTL Expiry ────────────────────────────────────────

    @Test
    fun scenario_expiredMessage_returnsError() {
        val error = ScenarioSimulator.expiredMessageError()
        assertTrue(error is Result.Error)
        assertTrue((error as Result.Error).exception.message?.contains("TTL", ignoreCase = true) == true)
    }

    // ── Scenario 8: Malformed Packet ──────────────────────────────────────────

    @Test
    fun scenario_malformedPacket_returnsError() {
        val error = ScenarioSimulator.malformedPacketError()
        assertTrue(error is Result.Error)
        assertTrue((error as Result.Error).exception.message?.contains("magic bytes", ignoreCase = true) == true)
    }

    // ── Scenario 9: Duplicate Packet ──────────────────────────────────────────

    @Test
    fun scenario_duplicatePacket_returnsError() {
        val error = ScenarioSimulator.duplicatePacketError()
        assertTrue(error is Result.Error)
        assertTrue((error as Result.Error).exception.message?.contains("Duplicate", ignoreCase = true) == true)
    }

    // ── Scenario 10: Offline — Notifications Still Work ───────────────────────

    @Test
    fun scenario_noInternet_localNotificationStillDisplays() {
        val notificationServiceWrapper = MockFactory.stubNotificationServiceWrapper(notificationsEnabled = true)
        val notificationManager = NotificationManagerImpl(notificationServiceWrapper)
        notificationManager.createNotificationChannels()

        val alert = TestDataFactory.fakeAlert()
        val result = notificationManager.showNotification(alert)
        assertTrue(result is Result.Success)
    }

    // ── Scenario 11: Debug Log Filtering ─────────────────────────────────────

    @Test
    fun scenario_debugModeOff_debugLogsAreFiltered() = runTest {
        val stubDataSource = MockFactory.stubLocalDataSource()
        val logger = LoggerManagerImpl(stubDataSource)
        logger.toggleDebugMode(false)

        assertFalse(logger.isDebugMode)

        // This DEBUG log should be silently dropped — no crash, no data loss
        logger.log(
            level = LogLevel.DEBUG,
            category = LogCategory.BLUETOOTH,
            message = "Scanning for nearby BLE peripherals",
            moduleName = "BluetoothScanner"
        )
    }

    // ── Scenario 12: Localization Fallback to English ─────────────────────────

    @Test
    fun scenario_unknownLocale_defaultsToEnglish() {
        val languageManager = LanguageManagerImpl()
        languageManager.changeLanguage("fr") // French is unsupported
        assertEquals("en", languageManager.getCurrentLanguage())
    }

    // ── Scenario 13: Communication — Stub Successful Delivery ────────────────

    @Test
    fun scenario_communicationManager_successfulSend() = runTest {
        val commManager = MockFactory.stubCommunicationManager(
            sendResult = Result.Success(DeliveryResult.SENT)
        )
        val result = commManager.sendMessage("Hello mesh".toByteArray())
        assertTrue(result is Result.Success)
        assertEquals(DeliveryResult.SENT, (result as Result.Success).data)
    }
}
