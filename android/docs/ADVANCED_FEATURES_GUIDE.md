# Advanced Features Guide
**Offline Emergency Mesh Communication System — Phase A32**

## Overview

Phase A32 introduced five advanced offline feature systems that enable the app to function as a complete emergency coordination tool without any internet connectivity.

---

## A32.1 — Offline Map Foundation

### Components
| File | Role |
|------|------|
| `MapRepository` | Interface for tile storage and layer configuration |
| `MapRepositoryImpl` | LRU tile cache (100 tiles max ≈ 10 MB) + default layers |
| `MapProviderImpl` | State machine: EMPTY → LOADING → LOADED/ERROR |
| `MapViewModel` | Combines layers + node positions + zoom state |
| `MapScreen` | Canvas-based grid map with gesture pan/zoom |

### How It Works
- No network tile fetching is ever performed
- Map tiles can be saved locally using `MapRepository.saveMapTile()`
- Default layers: `base`, `roads`, `nodes`, `emergency`, `elevation`
- Node positions are rendered as canvas circles at proportional coordinates

### Layer Management
```kotlin
mapProvider.setLayerVisible("elevation", true)  // Show/hide a layer
mapRepository.clearLayer("base")                // Remove cached tiles for a layer
```

---

## A32.2 — Location Sharing

### Components
| File | Role |
|------|------|
| `LocationPacket` | Serializable mesh protocol payload |
| `LocationSharingManager` | Interface for sharing and receiving locations |
| `LocationSharingManagerImpl` | Room-persisted + mesh-broadcast implementation |

### LocationPacket Protocol
```
[LOC]|senderId|latitude|longitude|accuracy|timestamp|altitude|label
```

Example:
```
[LOC]|node_001|23.8103|90.4125|15.0|1700000000000|10.0|HQ
```

### Receiving a location from mesh
```kotlin
val packet = LocationPacket.deserialize(rawPayload)
if (packet != null) {
    locationSharingManager.receiveLocationPacket(packet)
}
```

---

## A32.3 — Resource Sharing UI

### 5 Emergency Resource Categories
| Category | Key | Used For |
|----------|-----|----------|
| 🏥 Medical | MEDICAL | First aid, medications, stretchers |
| 🍞 Food | FOOD | Water, rations, supplies |
| 🏠 Shelter | SHELTER | Tents, blankets, structures |
| 🔧 Equipment | EQUIPMENT | Tools, generators, radios |
| 🤝 Service | SERVICE | Skills, transport, expertise |

### CRUD Operations
```kotlin
// Offer a resource
resourceManager.createOffer(
    name = "First Aid Kit",
    type = "MEDICAL",
    quantity = 3,
    latitude = 0.0, longitude = 0.0,
    description = "Full trauma kit",
    privacy = "PUBLIC"
)

// Request a resource
resourceManager.createRequest(type = "FOOD", requiredQuantity = 10, priority = "HIGH")

// Expire old resources
resourceManager.expireResources()
```

---

## A32.4 — Network Health Dashboard

Replaces the stub `NetworkScreen` with a live dashboard connected to `NetworkHealthManager`.

### Key Metrics
- **Connected Nodes** — count from `NetworkHealthManager.availableNodesCount`
- **Active Connections** — from `NetworkHealthManager.activeConnectionsCount`
- **Signal Quality** — from `NetworkHealthManager.averageSignalQuality`
- **Failure Rate** — from `NetworkHealthManager.networkFailureRate`

### Status Machine
```
NO_CONNECTION → failureRate checked → DEGRADED (>50%) or CONNECTED
```

---

## A32.5 — Node Visualization

Displays all known mesh nodes with filter chips: `All`, `Online`, `Offline`, `Relay`.

### Node Status Mapping
| Room Status | UI Color |
|-------------|----------|
| ONLINE | Green (connected) |
| WEAK_CONNECTION | Yellow (warning) |
| OFFLINE | Gray (offline) |

### Detail Sheet
Tap any node card to see: deviceId, type, RSSI, signal quality, battery, hop count, relay capability, last seen, and GPS coordinates if available.

---

## A32.6 — Offline Experience

### OfflineStatusManager
Tracks mesh connectivity state, sync queue size, and last sync time. Powers the offline indicator UI.

```kotlin
offlineStatusManager.setMeshConnected(true)   // Hides offline indicator
offlineStatusManager.setMeshConnected(false)  // Shows offline indicator
```

### SyncQueueManager
Queues operations for deferred delivery when mesh is unavailable.

```kotlin
val op = SyncOperation(id = uuid, type = SyncOperationType.MESSAGE, payload = bytes)
syncQueueManager.enqueue(op)  // Returns false if queue full (500 ops limit)
syncQueueManager.dequeueNext()  // FIFO delivery
syncQueueManager.clearAll()  // After bulk sync
```
