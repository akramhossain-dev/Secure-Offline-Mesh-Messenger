# Store & Forward Messaging System — Phase A11

## Store & Forward Architecture

In disaster response environments with sporadic network coverage, instant message delivery cannot be assumed. The Store & Forward architecture preserves packets locally and continuously attempts transmission when transceivers establish connection links:

```
                  ┌────────────────────────┐
                  │    Message Created     │
                  └───────────┬────────────┘
                              │
                    Attempt Delivery (instant)
                              │
              ┌───────────────┴───────────────┐
      Success │                       Failure │
     ┌────────▼────────┐             ┌────────▼────────┐
     │ Marked: SENT /  │             │ Store Database  │ (DbDeliveryStatus.PENDING)
     │ DELIVERED       │             └────────┬────────┘
     └─────────────────┘                      │
                                        Enqueue Queue
                                              │
                                     Periodic Retries /
                                    Backoff Sweeps (BG)
                                              │
                                     Forward to peer node
```

---

## Message Lifecycle States

Message packet lifecycle transitions map locally inside Room database [`MessageEntity`](../../android/app/src/main/java/com/mesh/emergency/data/local/entity/MessageEntity.kt) columns:

- `PENDING`: Newly created packet, waiting in local database memory.
- `QUEUED`: Enqueued in queue pipeline waiting for backoff interval clearance.
- `SENDING`: Actively writing to physical transceivers (Bluetooth/LoRa).
- `SENT`: Successfully broadcasted over the air.
- `DELIVERED`: Mesh acknowledgment packet returned from peer node.
- `FAILED`: Maximum retry threshold exceeded (e.g. 5 retries).
- `EXPIRED`: Time-to-Live (TTL) exceeded before delivery succeeded.

---

## Expiration & Time-To-Live (TTL)

To prevent the local device cache from filling up with old logs, messages declare lifetimes:
- **Emergency / SOS Alerts**: Long TTL (e.g. 7 days). Kept in memory as long as possible to maximize broadcast propagation.
- **Normal Chat Messages**: Short TTL (e.g. 24 hours). Automatically cleared if transceivers fail to discover candidates within the window.
- **Sweep Sweep**: Periodically triggered by WorkManager [`CleanupWorker`](../../android/app/src/main/java/com/mesh/emergency/core/communication/worker/CleanupWorker.kt).

---

## Exponential Backoff Retry Strategy

To prevent overloading radios (and draining battery) during disconnected periods, retries follow a strict backoff delay progression:

| Retry Count | Wait Required | Rationale |
|---|---|---|
| 0 | 0 seconds | Immediate transmission attempt. |
| 1 | 5 seconds | Brief pause for transient connection dropouts. |
| 2 | 30 seconds | Extended wait for peripheral restarts. |
| 3+ | 5 minutes | Low-power standby check interval. |

---

## Background Processing (WorkManager)

Background sweeps execute decoupled from the main thread lifecycle via Jetpack WorkManager:
1. **[`QueueProcessWorker`](../../android/app/src/main/java/com/mesh/emergency/core/communication/worker/QueueProcessWorker.kt)**: Fired when transceivers connect to scan the queue and retry.
2. **[`CleanupWorker`](../../android/app/src/main/java/com/mesh/emergency/core/communication/worker/CleanupWorker.kt)**: Periodically runs to mark messages past their `expiryTime` as `EXPIRED`.
