/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.designsystem.icon

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.BatteryUnknown
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SignalCellular0Bar
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.SignalCellular4Bar
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Standard icons provider mapping logical categories to Jetpack Compose Material Icons.
 */
object MeshIcons {

    // ── Navigation ────────────────────────────────────────────────────────────
    val Back: ImageVector    = Icons.Default.ArrowBack
    val Menu: ImageVector    = Icons.Default.Menu
    val Close: ImageVector   = Icons.Default.Close
    val Home: ImageVector    = Icons.Default.Home
    val Settings: ImageVector = Icons.Default.Settings

    // ── Communication ─────────────────────────────────────────────────────────
    val Chat: ImageVector    = Icons.Default.Message
    val Send: ImageVector    = Icons.Default.Send
    val Voice: ImageVector   = Icons.Default.Mic
    val Global: ImageVector  = Icons.Default.Public
    val QrScan: ImageVector  = Icons.Default.QrCode

    // ── Emergency ─────────────────────────────────────────────────────────────
    val Emergency: ImageVector = Icons.Default.Emergency
    val Warning: ImageVector   = Icons.Default.Warning
    val Info: ImageVector      = Icons.Default.Info
    val Alert: ImageVector     = Icons.Default.BatteryAlert

    // ── Network ───────────────────────────────────────────────────────────────
    val Bluetooth: ImageVector         = Icons.Default.Bluetooth
    val BluetoothDisabled: ImageVector = Icons.Default.BluetoothDisabled
    val SignalStrong: ImageVector      = Icons.Default.SignalCellular4Bar
    val SignalWeak: ImageVector        = Icons.Default.SignalCellularAlt
    val SignalNone: ImageVector        = Icons.Default.SignalCellular0Bar
    val NetworkCheck: ImageVector      = Icons.Default.NetworkCheck

    // ── User / Contacts ───────────────────────────────────────────────────────
    val User: ImageVector      = Icons.Default.Person
    val AddUser: ImageVector   = Icons.Default.PersonAdd

    // ── Status Indicators ─────────────────────────────────────────────────────
    val Check: ImageVector     = Icons.Default.Check
    val DoubleCheck: ImageVector = Icons.Default.DoneAll
    val Error: ImageVector     = Icons.Default.Error
    val Refresh: ImageVector   = Icons.Default.Refresh

    // ── Battery Status ────────────────────────────────────────────────────────
    val BatteryFull: ImageVector    = Icons.Default.BatteryFull
    val BatteryCharging: ImageVector = Icons.Default.BatteryChargingFull
    val BatteryLow: ImageVector     = Icons.Default.BatteryAlert
    val BatteryUnknown: ImageVector = Icons.Default.BatteryUnknown

    // ── Location ──────────────────────────────────────────────────────────────
    val Location: ImageVector = Icons.Default.LocationOn
    val Map: ImageVector      = Icons.Default.Map
    val MyLocation: ImageVector = Icons.Default.MyLocation
}
