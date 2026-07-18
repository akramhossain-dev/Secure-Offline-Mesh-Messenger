# Store & Forward System

The Store & Forward system ensures eventual message delivery when the target recipient is offline or outside mesh coverage.

---

## 1. Local Database Storage

If the `CommunicationManager` determines a target node is unreachable:
1. The packet is wrapped in a `pending_packet` database entity.
2. The entity is inserted into the local Room database.
3. The UI state updates the message display to `DeliveryStatus.QUEUED`.

### Database Schema Entity: `pending_packet`

| Column | Data Type | Key Constraints | Purpose |
|---|---|---|---|
| `packet_id` | TEXT | PRIMARY KEY | Unique packet UUID matching message ID. |
| `payload_json` | TEXT | NOT NULL | Serialized JSON representation of packet envelope. |
| `priority` | INTEGER | NOT NULL | Outbound queue priority. |
| `retry_count` | INTEGER | DEFAULT 0 | Count of connection attempts made. |
| `created_at` | INTEGER | NOT NULL | Creation epoch timestamp in ms. |
| `last_attempt_at` | INTEGER | DEFAULT 0 | Timestamp of last transmission attempt. |

---

## 2. Triggering Transmissions

Reattempting delivery is managed by two systems:

### A. Active Node Seen Events
When the Android application receives a `HELLO` packet indicating a previously offline contact has reconnected:
1. The `CommunicationManager` captures the node presence event.
2. `StoreForwardManager` queries the Room database for any `pending_packet` rows matching the target `node_id`.
3. Discovered packets are immediately pushed to the BLE/LoRa queue.

### B. WorkManager Periodic Polling Task
* A background `CoroutineWorker` runs a periodic polling check (default: every 10 seconds).
* The worker fetches pending critical and high-priority packets, checking if their target destinations have re-established contact.

---

## 3. Retirement & Cleanup Rules

To prevent the local database from filling up with dead packets, the `StoreForwardManager` enforces limits:

* **Max Retry Count**: Packets are retired and marked `FAILED` if their `retry_count` exceeds **20**.
* **Time-to-Live (TTL)**: Packets are automatically deleted from the database if `created_at` exceeds **24 hours**.
* **Manual Cleanup**: Users can clear the pending database cache via Settings.
