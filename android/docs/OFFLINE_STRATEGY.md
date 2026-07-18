# Offline Strategy Guide
**Offline Emergency Mesh Communication System — Architecture**

## Offline-First Principle

This application is **100% offline-first**. Internet connectivity is never required, assumed, or used. All data flows through:

1. **Room Database** (primary persistence layer)
2. **Mesh Network** (BLE/LoRa packet transport — simulated)
3. **In-Memory Caches** (LRU tile cache, sync queue, node registry)

---

## Data Flow

```
User Action
    │
    ▼
ViewModel (StateFlow)
    │
    ▼
Repository / Manager
    │
    ├── Room Database (persist)
    │
    └── CommunicationManager (broadcast to mesh)
              │
              ▼
        Other Mesh Nodes
```

---

## Offline Scenarios

### 1. Message Send (Mesh Unavailable)
```
User sends message
    → MessageRepository.sendMessage()
    → SyncQueueManager.enqueue(SyncOperation.MESSAGE)
    → UI shows "Queued" status
    → SyncQueueWorker picks up after 15 min (or when mesh restores)
```

### 2. Location Share (No GPS Hardware)
```
User sets manual location
    → LocationSharingManager.setCurrentLocation()
    → LocationSharingManager.shareCurrentLocation()
    → LocationPacket serialized and sent via CommunicationManager
    → Other nodes receive [LOC] prefix, call receiveLocationPacket()
```

### 3. Resource Offer
```
User creates offer
    → ResourceManager.createOffer()
    → Room DB: ResourceEntity saved
    → ResourceExpiryWorker checks TTL every 6 hours
    → Expired resources auto-set to UNAVAILABLE
```

---

## Queue Management

The `SyncQueueManager` holds up to **500 pending operations**:

| Priority | SyncOperationType |
|----------|-------------------|
| Highest | EMERGENCY |
| High | MESSAGE |
| Normal | LOCATION |
| Low | RESOURCE, ACK |

Operations are retried up to **3 times** before being dropped.

---

## Data Retention Policies

| Entity | TTL |
|--------|-----|
| Messages | 30 days (configurable) |
| Locations | No expiry |
| Resources | Configurable TTL per resource |
| Log entries | 7 days (LogCleanupWorker) |
| Map tiles | No expiry (LRU eviction only) |

---

## No Internet Dependencies

✅ No external APIs called  
✅ No CDN or map tile server  
✅ No cloud authentication  
✅ No telemetry or analytics  
✅ No push notifications via internet  
✅ All encryption is local (AES-256)
