/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.power

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.mesh.emergency.core.power.BatteryModel
import com.mesh.emergency.core.power.BatteryProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real implementation of [BatteryProvider] reading live battery data from the Android
 * [BatteryManager] system service and the ACTION_BATTERY_CHANGED sticky broadcast.
 *
 * Replaces [BatteryProviderStub] in production DI binding.
 */
@Singleton
class BatteryProviderImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : BatteryProvider {

    private val batteryManager: BatteryManager? by lazy {
        context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
    }

    override fun getBatteryStatus(): Flow<BatteryModel> = callbackFlow {
        // Emit initial state from sticky broadcast immediately
        val initialIntent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        initialIntent?.let { trySend(buildBatteryModel(it)) }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                intent?.let { trySend(buildBatteryModel(it)) }
            }
        }
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        awaitClose { context.unregisterReceiver(receiver) }
    }.distinctUntilChanged()

    override fun isCharging(): Boolean {
        val bm = batteryManager ?: return false
        val status = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
    }

    override fun getBatteryLevel(): Int {
        return batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 0
    }

    private fun buildBatteryModel(intent: Intent): BatteryModel {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        val percentage = if (scale > 0) (level * 100 / scale) else level

        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        val powerSource = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC      -> "ac"
            BatteryManager.BATTERY_PLUGGED_USB     -> "usb"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "wireless"
            else                                   -> "battery"
        }

        val health = when (intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)) {
            BatteryManager.BATTERY_HEALTH_GOOD        -> "good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT    -> "overheat"
            BatteryManager.BATTERY_HEALTH_DEAD        -> "dead"
            BatteryManager.BATTERY_HEALTH_COLD        -> "cold"
            else                                      -> "unknown"
        }

        val temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10.0f

        return BatteryModel(
            level = percentage,
            isCharging = isCharging,
            powerSource = powerSource,
            health = health,
            temperature = temperature,
            timestamp = System.currentTimeMillis()
        )
    }
}
