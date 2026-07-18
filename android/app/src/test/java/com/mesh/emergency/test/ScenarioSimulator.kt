/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.test

import com.mesh.emergency.core.common.result.Result
import com.mesh.emergency.core.communication.DeliveryResult

/**
 * Offline and failure simulation helpers for integration scenario tests.
 *
 * These helpers provide consistent error-producing stubs to test the system
 * under adverse offline conditions without relying on real hardware.
 */
object ScenarioSimulator {

    // ── Connection Failures ───────────────────────────────────────────────────

    /** Simulates a complete Bluetooth adapter power-off scenario. */
    fun bluetoothOffError(): Result<DeliveryResult> =
        Result.Error(Exception("BLE adapter is disabled"))

    /** Simulates a LoRa radio unavailable scenario. */
    fun loraUnavailableError(): Result<DeliveryResult> =
        Result.Error(Exception("LoRa transport not initialised"))

    /** Simulates a general packet transmission failure. */
    fun packetSendFailed(): Result<DeliveryResult> =
        Result.Error(Exception("Packet transmission failed: ACK timeout"))

    /** Simulates a connection timeout after N milliseconds. */
    fun connectionTimeoutError(timeoutMs: Long = 30_000L): Result<DeliveryResult> =
        Result.Error(Exception("Connection timed out after ${timeoutMs}ms"))

    /** Simulates a device-not-reachable error after all retry attempts. */
    fun deviceUnreachableError(): Result<DeliveryResult> =
        Result.Error(Exception("Device not reachable: max retries exhausted"))

    // ── Permission Failures ───────────────────────────────────────────────────

    /** Simulates an OS-level permission denial error for Bluetooth. */
    fun bluetoothPermissionDenied(): Result<Unit> =
        Result.Error(SecurityException("BLUETOOTH_SCAN permission denied by user"))

    /** Simulates an OS-level permission denial error for Location. */
    fun locationPermissionDenied(): Result<Unit> =
        Result.Error(SecurityException("ACCESS_FINE_LOCATION permission denied by user"))

    // ── Storage Failures ──────────────────────────────────────────────────────

    /** Simulates a local database write failure. */
    fun databaseWriteFailed(): Result<Unit> =
        Result.Error(Exception("Room database write failed: disk full"))

    /** Simulates a storage quota exceeded scenario. */
    fun storageQuotaExceeded(): Result<Unit> =
        Result.Error(Exception("Insufficient storage space"))

    // ── Low Battery ───────────────────────────────────────────────────────────

    /** Battery level below critical threshold (below 10%). */
    const val CRITICAL_BATTERY_LEVEL = 9

    /** Battery level at warning threshold (below 20%). */
    const val WARNING_BATTERY_LEVEL = 19

    /** Fully charged battery level. */
    const val FULL_BATTERY_LEVEL = 100

    // ── Message Protocol Failures ─────────────────────────────────────────────

    /** Simulates a message that has exceeded its TTL. */
    fun expiredMessageError(): Result<Unit> =
        Result.Error(Exception("Message TTL expired: dropping from queue"))

    /** Simulates an invalid packet structure that fails deserialization. */
    fun malformedPacketError(): Result<Unit> =
        Result.Error(Exception("Packet deserialization failed: invalid magic bytes"))

    /** Simulates a duplicate packet already present in mesh routing table. */
    fun duplicatePacketError(): Result<Unit> =
        Result.Error(Exception("Duplicate packet detected: discarding"))
}
