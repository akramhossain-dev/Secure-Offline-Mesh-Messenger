# User Flow

**Project:** Offline Emergency Mesh Communication System  

---

## Flow 1 — First Launch & Identity Setup

```
Open App
    │
    ▼
[First launch?]
    │
    ├─ YES ──► Onboarding Screen
    │               │
    │               ▼
    │          Enter Display Name
    │               │
    │               ▼
    │          Generate Node ID (UUID)
    │               │
    │               ▼
    │          Generate ECDH Key Pair
    │               │
    │               ▼
    │          Set Language & Visibility
    │               │
    │               ▼
    │          Identity Stored (Room DB)
    │               │
    │               ▼
    └─ NO ───► Home Dashboard
```

---

## Flow 2 — Normal Communication

```
Open App
    │
    ▼
Home Dashboard
    │
    ▼
Open Conversation or Discover Nearby Users
    │
    ▼
[Contact Exists?]
    │
    ├─ NO ──► QR Pair / Nearby Discovery ──► Add Contact
    │
    └─ YES
        │
        ▼
   Type Message → Tap Send
        │
        ▼
   Communication Manager
        │
        ├─ [Receiver in BLE Range?]
        │       YES ──► Bluetooth Transport ──► ESP32 ──► Delivered
        │
        ├─ [Receiver on LoRa Mesh?]
        │       YES ──► LoRa Transport ──► ESP32 ──► Mesh ──► Delivered
        │
        └─ [Receiver Offline?]
                YES ──► Store & Forward Cache
                                │
                                ▼
                         Retry on reconnection
                                │
                                ▼
                         Delivered
```

---

## Flow 3 — Emergency SOS

```
Any Screen (emergency button visible)
    │
    ▼
User Presses SOS Button
    │
    ▼
Confirmation Dialog ("Confirm SOS?")
    │
    ├─ CANCEL ──► Return to previous screen
    │
    └─ CONFIRM
        │
        ▼
   Emergency Status Activated (user status = EMERGENCY)
        │
        ▼
   Location Permission Check
        │
        ├─ NOT GRANTED ──► Request Permission
        │                       │
        │                       ├─ DENIED ──► SOS sent without coordinates
        │                       │
        │                       └─ GRANTED ──► Continue
        │
        └─ GRANTED
            │
            ▼
       GPS Coordinates Captured
            │
            ▼
       SOS Packet Created (priority = CRITICAL)
            │
            ▼
       Broadcast via ALL available transports
            │
            ▼
       Nearby Rescue Nodes Notified (full-screen alert)
            │
            ▼
       SOS Active Screen (60s repeat timer)
            │
            ▼
       [User Cancels?]
            │
            ├─ YES ──► Status reset → Cancellation broadcast
            │
            └─ NO  ──► Repeat every 60 seconds
```

---

## Flow 4 — Receive Incoming Message

```
ESP32 Receives LoRa Packet
    │
    ▼
Delivered to Android App via BLE Notify
    │
    ▼
[Packet Type?]
    │
    ├─ TEXT ──► Decrypt with private key ──► Store in Room DB ──► Notification
    │
    ├─ GLOBAL_CHAT ──► Store in Room DB ──► Notification
    │
    ├─ SOS ──► Full-screen alert ──► Map marker placed ──► Notification
    │
    ├─ LOCATION ──► Update location_share table ──► Map marker updated
    │
    ├─ VOICE ──► Buffer chunks ──► Reassemble ──► Auto-play
    │
    └─ RESOURCE ──► Aggregate to Resource Board
```

---

## Flow 5 — Location Sharing

```
Map Screen
    │
    ▼
Tap "Share My Location"
    │
    ▼
Select Duration (15 min / 1 hour / Until stopped)
    │
    ▼
GPS Permission Check
    │
    └─ GRANTED
        │
        ▼
   GPS Coordinates Captured
        │
        ▼
   LOCATION Packet Sent (to selected contacts or broadcast)
        │
        ▼
   Status Bar Badge (countdown timer)
        │
        ▼
   Repeat every 60 seconds
        │
        ▼
   [Timer expires or user stops]
        │
        ▼
   Location sharing ends — no further packets sent
```

---

## Flow 6 — QR Code Pairing

```
User A (shows QR)                    User B (scans QR)
      │                                      │
      ▼                                      ▼
Open Profile Screen              Open Contacts → Scan QR
      │                                      │
      ▼                                      ▼
Display QR Code                    Scan User A's QR
(NodeID:PublicKey)                           │
                                             ▼
                                   Parse NodeID + PublicKey
                                             │
                                             ▼
                                   Store User A as Contact
                                   (with public key)
                                             │
                                             ▼
                                   Send HELLO packet
                                   (User B's NodeID:PublicKey)
      │                                      │
      ▼                                      │
Receive HELLO ◄──────────────────────────────┘
      │
      ▼
Store User B as Contact
      │
      ▼
Mutual contact established
Private chat unlocked (E2E encrypted)
```

---

## Flow 7 — Store & Forward Delivery

```
User A sends message to User B (User B is OFFLINE)
    │
    ▼
Communication Manager → Store & Forward
    │
    ▼
Message stored in pending_packet (Room DB)
    │
    ▼
Background worker polls every 10 seconds
    │
    ▼
[User B comes online — HELLO packet received]
    │
    ▼
StoreForwardManager triggered
    │
    ▼
Cached packets for User B retrieved
    │
    ▼
Re-transmitted via LoRa Transport
    │
    ▼
ACK received from User B's node
    │
    ▼
pending_packet deleted
    │
    ▼
Delivery status → DELIVERED shown in User A's UI
```

---

## Flow 8 — Settings & Configuration

```
Home Dashboard → Settings
    │
    ▼
[Setting changed]
    │
    ├─ Display Name → Updated in Room DB → Propagated via next HELLO packet
    │
    ├─ BLE Visibility (Public/Private) → Controls BLE advertising
    │
    ├─ Message TTL → Sets default hop limit for outbound packets
    │
    ├─ SOS Repeat Interval → WorkManager schedule updated
    │
    ├─ Store & Forward → Enable/disable local caching
    │
    ├─ INA219 Polling → Enable/disable power monitoring
    │
    └─ Clear Data → Wipe Room DB (messages + contacts) after confirmation
```

---

## Related Documents

- [Use Cases](use-cases.md)
- [Screen Documentation](../app/screen-documentation.md)
- [Communication Manager](../app/communication-manager.md)
- [Hybrid Communication](../communication/hybrid-communication.md)
