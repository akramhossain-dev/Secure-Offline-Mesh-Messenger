# Performance Optimization Guide
**Offline Emergency Mesh Communication System — Phase A33**

## Overview

Phase A33 systematically optimizes startup time, battery usage, database performance, memory, background tasks, and UI rendering.

---

## A33.1 — App Startup Optimization

### AppStartupOptimizer
Separates initialization into two phases to reduce time-to-first-frame.

```kotlin
// In Application.onCreate() — MAIN THREAD, must be < 50ms
AppStartupOptimizer.initialize(context)

// After first frame — BACKGROUND THREAD
lifecycleScope.launch(Dispatchers.IO) {
    AppStartupOptimizer.initDeferred(context)
}
```

### measureBlock Utility
```kotlin
val result = measureBlock("DatabaseQuery") {
    database.nodeDao().getNetworkNodes()
}
// Logs: "DatabaseQuery: 12ms"
```

---

## A33.2 — Battery Optimization

### BatteryAwareScheduler

Adjusts scan intervals and DB batch sizes based on current `PowerSavingMode`.

| PowerSavingMode | BLE Scan | Location | DB Batch |
|-----------------|----------|----------|----------|
| PERFORMANCE | 5s | 10s | 100 |
| BALANCED | 15s | 30s | 50 |
| AGGRESSIVE_SAVE | 60s | 2min | 20 |
| ULTRA_SAVE | Disabled | Disabled | 10 |

```kotlin
// Check before expensive operation
if (batteryScheduler.isHeavyOperationAllowed) {
    performExpensiveSync()
}

// Get current scan interval
val interval = batteryScheduler.bleScanIntervalMs

// Run only if battery allows
val result = batteryScheduler.runIfAllowed { heavyOperation() }
```

---

## A33.3 — Database Optimization

### Room Indexes Added

| Entity | Index Columns |
|--------|---------------|
| `messages` | `conversationId`, `timestamp` |
| `network_nodes` | `deviceId`, `lastSeen` |
| `resources` | `type`, `availabilityStatus` |
| `locations` | `userId`, `timestamp` |

### WAL Mode (Applied via DatabaseModule callback)
```sql
PRAGMA journal_mode=WAL     -- Concurrent reads during writes
PRAGMA cache_size=4096      -- 4 MB page cache
PRAGMA synchronous=NORMAL   -- Faster writes, still safe
```

### Paginated Queries
```kotlin
// MessageDao
dao.getPagedMessages(convId = "conv_001", limit = 20, offset = 0)

// ResourceDao
dao.getPagedResources(type = "MEDICAL", limit = 20, offset = 0)
```

### DatabaseOptimizer Helpers
```kotlin
val offset = DatabaseOptimizer.offsetFor(page = 2)     // → 40
val pages  = DatabaseOptimizer.totalPages(totalCount = 85) // → 5
DatabaseOptimizer.logQueryTime("NodeQuery", startMs)   // Warns if > 100ms
```

---

## A33.4 — Memory Optimization

### ImageCacheManager
LRU cache backed by `android.util.LruCache` using 1/8th of heap.

```kotlin
val cache = ImageCacheManager()  // auto-sizes to 1/8 heap
cache.put("node_avatar_001", bitmap)
val cached = cache.get("node_avatar_001")  // null if evicted
cache.evictAll()  // on low-memory callback
println(cache.stats())
```

---

## A33.5 — Background Tasks

### MeshWorkManager

Central coordinator for all periodic workers.

```kotlin
// Register all workers (safe to call multiple times)
meshWorkManager.scheduleAllWorkers()

// Cancel all (e.g., on signout)
meshWorkManager.cancelAllWorkers()
```

### Worker Schedule

| Worker | Interval | Constraint | Purpose |
|--------|----------|------------|---------|
| `ResourceExpiryWorker` | 6 hours | Battery not low | Expire old resources |
| `LogCleanupWorker` | 24 hours | None | Delete logs > 7 days |
| `SyncQueueWorker` | 15 min | Battery not low | Drain offline queue |
| `BatteryOptimizationWorker` | 30 min | None | Log power mode state |

---

## A33.6 — UI Performance

### @Stable / @Immutable Models
Prevent unnecessary Compose recompositions by decorating UI data classes:

```kotlin
@Immutable
data class ImmutableList<T>(val items: List<T>)

@Stable
data class StableNodeItem(val id: String, ...)
```

**Usage in LazyColumn:**
```kotlin
LazyColumn {
    items(
        items = stableNodes.items,
        key = { it.id }  // Always use stable keys!
    ) { node ->
        NodeCard(node = node)
    }
}
```

### key= on all list items
Every `LazyColumn`/`LazyRow` in A32 screens uses `key = { it.id }` to prevent unnecessary recompositions on partial list updates.

---

## A33.7 — Error Handling

### CrashHandler
```kotlin
// Install in Application.onCreate(), AFTER Timber.plant()
CrashHandler.install()
```

### OfflineFailureHandler
```kotlin
// Observe from ViewModel
offlineFailureHandler.failureEvents.collect { event ->
    when (event) {
        is OfflineFailureEvent.QueueOverflow -> showSnackbar("Queue full")
        is OfflineFailureEvent.StorageLow -> showSnackbar("Storage low")
        is OfflineFailureEvent.DecryptionFailed -> logError(event.messageId)
        is OfflineFailureEvent.DatabaseError -> logError(event.reason)
        is OfflineFailureEvent.RecoverySuggestion -> showDialog(event.message)
    }
}
```

---

## A33.8 — Tests Added

| Test File | Coverage |
|-----------|----------|
| `OfflineMapTest.kt` | MapRepository CRUD, layer toggle, bounds |
| `LocationStorageTest.kt` | LocationPacket round-trip, null handling |
| `NetworkDashboardTest.kt` | SyncQueue FIFO, capacity limits, OfflineStatus |
| `PerformanceTest.kt` | LRU eviction, pagination math, timing |
| `BatteryBehaviorTest.kt` | All 4 power modes, scan gates, DB batch |
