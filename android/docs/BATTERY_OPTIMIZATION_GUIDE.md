# Battery Optimization Guide
**Offline Emergency Mesh Communication System — Phase A33.2**

## Overview

Emergency devices often run on limited battery. The BatteryAwareScheduler intelligently throttles background operations based on `PowerSavingMode` provided by `PowerManager` (A19).

---

## Power Saving Modes

| Mode | Description |
|------|-------------|
| `PERFORMANCE` | Full functionality — device plugged in or battery > 80% |
| `BALANCED` | Normal operation — battery 20–80% |
| `AGGRESSIVE_SAVE` | Reduced scans — battery < 20% |
| `ULTRA_SAVE` | Critical state — battery < 5%, mesh scanning suspended |

---

## Operation Intervals Per Mode

| Operation | PERFORMANCE | BALANCED | AGGRESSIVE | ULTRA_SAVE |
|-----------|-------------|----------|------------|------------|
| BLE Scan | 5s | 15s | 60s | **Disabled** |
| Location Update | 10s | 30s | 2 min | **Disabled** |
| DB Batch Size | 100 | 50 | 20 | 10 |

---

## Integration Pattern

```kotlin
// In any manager/repository that does background scanning:

@Inject lateinit var batteryScheduler: BatteryAwareScheduler

suspend fun startScan() {
    if (!batteryScheduler.isScanningAllowed) {
        Timber.d("Scan blocked — battery saver active")
        return
    }
    val interval = batteryScheduler.bleScanIntervalMs
    // Configure BLE scan with this interval
}

suspend fun processData() {
    val result = batteryScheduler.runIfAllowed {
        expensiveDatabaseOperation()
    }
    // Result is Error if battery saver blocked the operation
}
```

---

## WorkManager Battery Constraints

All workers are configured with `setRequiresBatteryNotLow(true)` (except log cleanup which runs regardless):

```kotlin
val constraints = Constraints.Builder()
    .setRequiresBatteryNotLow(true)
    .setRequiresStorageNotLow(true)
    .build()
```

This ensures background tasks pause automatically when battery is critically low — no manual checking needed.

---

## Recommendations for Emergency Use

1. **Enable airplane mode** — reduces radio interference, extends battery
2. **Use PERFORMANCE mode** only when charging or with backup power
3. **SyncQueueWorker** ensures no messages are lost during ULTRA_SAVE mode
4. **BatteryOptimizationWorker** logs power mode every 30 minutes for diagnostics
5. If battery drops below 5%, **only EMERGENCY priority messages** should be sent
