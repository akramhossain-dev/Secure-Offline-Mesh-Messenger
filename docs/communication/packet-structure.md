# Packet Structure

All messages traveling across the mesh network are formatted as JSON envelopes to facilitate ease of debugging, modularity, and parsing efficiency.

---

## 1. JSON Envelope Specification

Here is an example of a canonical JSON packet:

```json
{
  "id": "e4b2d180-a3e9-4e78-9e55-cb88523ad99c",
  "type": "TEXT",
  "sender": "a3f2c1d0-8b4e-4a7c-9f1e-2b3c4d5e6f70",
  "receiver": "e4b2d180-c119-4822-9f33-728b9c4cdd2a",
  "timestamp": 1752748800000,
  "priority": 2,
  "ttl": 5,
  "hop_count": 0,
  "payload": "SGVsbG8gV29ybGQ=",
  "signature": "base64-hmac-sha256-signature"
}
```

---

## 2. Header and Field Definitions

| Field | JSON Type | Size (typical) | Purpose |
|---|---|---|---|
| **id** | String (UUID) | 36 bytes | Unique packet identifier. Used for seen-cache check and deduplication. |
| **type** | String (Enum) | 4–12 bytes | Message type (determines routing priority and payload format). |
| **sender** | String (UUID) | 36 bytes | Node ID of the originating device. |
| **receiver** | String | 36 bytes | Target Node's ID or `"BROADCAST"`. |
| **timestamp** | Integer (Epoch) | 8 bytes | Unix epoch timestamp in milliseconds. |
| **priority** | Integer (Enum) | 1 byte | Routing queue priority (0 = Critical, 1 = High, 2 = Normal, 3 = Low). |
| **ttl** | Integer | 1 byte | Time-To-Live hop limit (decrements on relay, dropped at 0). |
| **hop_count** | Integer | 1 byte | Tracks number of relay steps traveled (increments on relay). |
| **payload** | String (Base64) | up to 200 bytes | Encrypted payload (private chat) or plaintext Base64 data (broadcast). |
| **signature** | String (Base64) | 44 bytes | HMAC-SHA256 signature verifying integrity. |

---

## 3. Message Types

### A. `TEXT` (Priority: 2)
* Used for E2E encrypted private chat messages.
* Payload contains AES-256-GCM ciphertext + IV.

### B. `VOICE` (Priority: 2)
* Used for chunked audio voice clips.
* Payload is structured: `{ "msg_id": "uuid", "seq": 0, "total": 4, "data": "base64-chunk" }`.

### C. `LOCATION` (Priority: 1)
* Explicit, time-limited location coordinate updates.
* Payload contains: `{ "lat": 23.8103, "lon": 90.4125, "accuracy": 10.0, "expires": 1752752400000 }`.

### D. `SOS` (Priority: 0)
* Critical emergency alert with highest routing priority.
* Payload contains coordinates, distress message text, and device battery level.

### E. `RESOURCE` (Priority: 2)
* Resource offers shared with the community.
* Payload contains resource category enum, quantity description, and location text.

### F. `STATUS` (Priority: 3)
* Heartbeat packet used for device health and network discovery.
* Contains node name, visibility configs, and power levels.
