# Features

---

## Feature Summary

| Category | Feature | Status |
|---|---|---|
| User System | Profile and unique ID | Core |
| User System | QR Code pairing | Core |
| User System | Nearby device discovery | Core |
| User System | Contact management | Core |
| Messaging | Private chat | Core |
| Messaging | Global mesh chat | Core |
| Messaging | Emergency broadcast | Core |
| Messaging | Message history | Core |
| Messaging | Voice messages | Advanced |
| Emergency | SOS system | Core |
| Emergency | Emergency status flag | Core |
| Emergency | Location sharing | Core |
| Emergency | Offline maps | Advanced |
| Emergency | Resource sharing | Advanced |
| Emergency | Trusted rescue network | Advanced |
| Routing | Store and Forward | Core |
| Routing | Multi-hop mesh relay | Core |
| Routing | Smart message priority | Advanced |
| Privacy | Permission-based location | Core |
| Privacy | User-controlled visibility | Core |
| Monitoring | Power telemetry (INA219/226) | Core |
| Monitoring | Node health status | Core |
| Localization | Multi-language support | Advanced |

---

## User System

### Unique User ID
Each user is assigned a UUID at first launch. This ID is:
- Generated locally — no server involved
- Embedded in every outbound packet as the `sender` field
- Persisted in local Room Database
- Used to derive the user's public/private key pair

### QR Code Pairing
Users share their Node ID and public key by displaying a QR code. Scanning another user's QR code:
1. Adds them to the local contact list
2. Stores their public key for encrypted messaging
3. Establishes a named contact entry in Room Database

### Nearby Device Discovery
The ESP32 broadcasts a BLE advertisement with its Node ID. The Android app scans for known and unknown nodes. Discovered nodes are presented as "nearby" users in the contact discovery screen.

### Contact Management
Contacts are stored locally with:
- Display name (user-assigned)
- Node ID (UUID)
- Public key (for E2E encryption)
- Last seen timestamp
- Status flags (rescue team, coordinator, etc.)

---

## Messaging

### Private Chat
Point-to-point messages encrypted with the recipient's public key. Only the intended recipient can decrypt the payload. The `receiver` field in the packet header carries the destination Node ID.

### Global Mesh Chat
Packets with `receiver = "BROADCAST"` are distributed to all nodes within the mesh. No encryption is applied to global messages — all nodes can read them. Global chat supports group coordination during emergencies.

### Emergency Broadcast
A high-priority broadcast that preempts all queued packets. Emergency broadcasts are:
- Forwarded by every node regardless of TTL state
- Displayed with a full-screen alert on the receiving Android app
- Stored and retransmitted by store-and-forward until acknowledged

### Message History
All messages (sent and received) are persisted in the local Room Database. History is available offline at any time. Messages are indexed by conversation thread, sorted by timestamp.

### Voice Messages
Audio is recorded on the Android device, compressed, chunked into LoRa-compatible packet sizes, and transmitted as a sequence of VOICE packets with sequence numbers. The receiving app reassembles and plays back the audio.

---

## Emergency Features

### SOS System
Pressing the SOS button:
1. Sets the user's emergency status flag
2. Sends a high-priority SOS packet with current GPS coordinates
3. Repeats the SOS broadcast every 60 seconds until cancelled
4. Other nodes store and forward the SOS across the mesh

### Emergency Status
Users can set themselves as "Emergency" or "Rescue" status. This status is embedded in HELLO packets and visible to all mesh participants. Rescue-status users receive priority routing.

### Location Sharing
GPS coordinates are included in LOCATION packets. Sharing is:
- Explicit — the user initiates it
- Time-limited — coordinates expire after a configurable duration
- Visible only to contacts with permission granted

### Offline Maps
The app includes a pre-loaded offline map (OpenStreetMap tiles). Contacts with active location sharing appear as map markers. The map works entirely without internet.

### Resource Sharing
Users broadcast available resources (water, medical supplies, shelter capacity) as RESOURCE packets. Resource listings are aggregated in the app's Resource Board and forwarded across the mesh.

### Trusted Rescue Network
Rescue-flagged contacts form a priority communication layer. Their messages receive elevated routing priority. Private channels can be established between rescue team members using mutual key authentication.

---

## Routing and Network Features

### Store and Forward
When a destination node is unreachable, the packet is cached in the ESP32's PSRAM or SPIFFS. The cache is periodically retried when new nodes appear in range. TTL governs maximum retry lifespan.

### Multi-Hop Mesh Relay
Every node participates in routing. Packets are rebroadcast with decremented TTL. A seen-packet cache (keyed by sender UUID + message ID) prevents duplicate forwarding.

### Smart Message Priority
Packets are queued with priority levels:

| Priority Level | Packet Types |
|---|---|
| Critical | SOS, Emergency Broadcast |
| High | ACK, LOCATION (rescue) |
| Normal | TEXT, VOICE, RESOURCE |
| Low | HELLO, telemetry |

---

## Privacy Features

### Permission-Based Location
Location is never shared automatically. The user must explicitly tap "Share Location" to send coordinates. A countdown timer limits the duration of active sharing.

### No Background Tracking
The Android app does not access GPS in the background unless the user has activated SOS mode. No location data is transmitted without an explicit user action.

### User-Controlled Visibility
Users control whether their Node ID is advertised in BLE discovery. Setting visibility to "private" suppresses BLE advertising; the node still receives mesh traffic but does not announce itself.

---

## Power Monitoring

### INA219 / INA226 Integration
The current sensor is polled every 5 seconds by the ESP32. Readings are:
- Logged to SPIFFS for historical trending
- Transmitted to the Android app via BLE status notification
- Displayed on the app's Device Health screen

| Monitored Value | Source | Unit |
|---|---|---|
| Bus voltage | INA219/226 | V |
| Shunt current | INA219/226 | mA |
| Power consumption | Calculated | mW |
| Estimated battery remaining | Calculated from Ah | % |
