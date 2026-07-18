# Development Roadmap

---

## Overview

Development is organized into three parallel tracks that can progress independently with defined integration checkpoints.

| Phase | Track | Goal |
|---|---|---|
| Phase A | Android App | Complete, deployable Android application |
| Phase B | ESP32 Firmware | Complete, field-ready embedded firmware |
| Phase H | Hardware | Validated, reproducible hardware assembly |

---

## Development Order

The recommended build sequence is:

| Step | Activity |
|---|---|
| 1 | Build Android App UI and architecture |
| 2 | Develop Bluetooth communication |
| 3 | Develop ESP32 firmware |
| 4 | Integrate LoRa communication |
| 5 | Implement mesh protocol |
| 6 | Add security |
| 7 | Add power monitoring |
| 8 | Field testing |

---

## Phase A — Android App Development

**Goal:** A fully functional Android app that can communicate with an ESP32 node over BLE, send and receive LoRa mesh messages, and support all emergency features.

---

### A1 — Project Foundation

| Task | Details |
|---|---|
| Project setup | Kotlin, Gradle, min SDK 26, target SDK 34 |
| Architecture skeleton | MVVM + Clean Architecture package structure |
| Dependency injection | Hilt setup with all module definitions |
| Navigation | NavHost with all route definitions (empty screens) |
| Room Database | Schema v1: user, contact, message, conversation tables |
| Room Database | Migration framework setup |
| CI/CD | GitHub Actions: build + lint on every PR |

**Dependencies:** None  
**Testing Criteria:**
- App builds without errors
- Navigation between all route destinations works
- Room Database creates schema without errors (instrumented test)

---

### A2 — User Identity and Pairing

| Task | Details |
|---|---|
| Node ID generation | UUID v4 generated at first launch, persisted in Room |
| ECDH key pair generation | P-256 key pair, private key in Android Keystore |
| Profile screen | Display name, avatar, Node ID display |
| QR code generation | `NodeID:PublicKey` encoded in QR |
| QR code scanner | CameraX + ZXing-based scanner |
| Contact storage | Parse and store Node ID + public key from scanned QR |
| BLE advertiser scanner | Discover nearby ESP32 nodes by BLE advertisement |

**Dependencies:** A1  
**Testing Criteria:**
- Node ID persists across app restarts
- QR code correctly encodes and decodes NodeID + PublicKey
- Scanning a QR code adds a contact to Room Database

---

### A3 — BLE Communication Layer

| Task | Details |
|---|---|
| BLE Central setup | GATT connection to paired ESP32 |
| GATT service discovery | Discover TX, RX, Status characteristics |
| Write characteristic | Send outbound packets to ESP32 TX characteristic |
| Notify characteristic | Receive inbound packets via GATT notifications |
| Connection state management | Reconnect on disconnection with exponential backoff |
| Packet serializer | Serialize/deserialize JSON packets |
| BLE state UI | Connection status indicator in app status bar |

**Dependencies:** A2  
**Testing Criteria:**
- App connects to ESP32 within 5 seconds of launch
- Outbound packets are written to ESP32 TX characteristic without error
- Inbound BLE notifications are received and deserialized correctly

---

### A4 — Messaging Core

| Task | Details |
|---|---|
| Private chat screen | Compose chat UI with message bubbles |
| Message send flow | Text → encrypt → serialize → BLE write |
| Message receive flow | BLE notify → deserialize → decrypt → Room insert |
| Conversation list | Real-time Room Flow → Compose UI |
| Message status updates | Queued / Sent / Delivered indicators |
| Global chat screen | Broadcast send and receive |
| Message history | Persistent Room storage, indexed by conversation |

**Dependencies:** A3  
**Testing Criteria:**
- Private message encrypts with recipient's key and decrypts correctly
- Message appears in conversation thread within 1 second of receipt
- Delivery status updates to Delivered when ACK is received

---

### A5 — Emergency Features

| Task | Details |
|---|---|
| SOS button and confirmation | Two-step confirm dialog, SOS packet send |
| SOS repeat mechanism | WorkManager periodic worker (60s interval) |
| Emergency status flags | Status field in HELLO packet, badge in UI |
| Emergency broadcast | Full-mesh broadcast with confirmation dialog |
| Location service | FusedLocationProviderClient, explicit permission |
| Location sharing | LOCATION packet, time-limited, Room storage |
| Offline map | OsmDroid integration, bundled tile set |
| Contact map markers | Active location shares plotted on map |

**Dependencies:** A4  
**Testing Criteria:**
- SOS repeat broadcasts exactly every 60 seconds until cancelled
- Location coordinates are embedded correctly in LOCATION packets
- Offline map renders without internet connection

---

### A6 — Advanced Features

| Task | Details |
|---|---|
| Voice messages | MediaRecorder → AAC → chunked VOICE packets |
| Voice playback | Chunk reassembly → MediaPlayer |
| Resource board | RESOURCE packet send/receive, board UI |
| Trusted rescue network | Rescue status contact filtering |
| Power monitor screen | Real-time voltage/current from BLE status |
| Multi-language | strings.xml for EN, AR, FR, ES, UR, BN |
| Settings screen | All configurable parameters |
| Notifications | Incoming message, SOS, BLE disconnect alerts |

**Dependencies:** A5  
**Testing Criteria:**
- Voice message records, transmits (chunked), and plays back correctly
- Resource board aggregates RESOURCE packets from multiple contacts
- Power monitor screen updates every 5 seconds with live data

---

## Phase B — ESP32 Firmware Development

**Goal:** Stable, tested firmware that bridges BLE to LoRa, implements mesh routing, and runs reliably on a Power Bank power source.

---

### B1 — Development Environment

| Task | Details |
|---|---|
| PlatformIO project setup | `platformio.ini` with ESP32 target |
| Library dependencies | LoRa, ArduinoJson, NimBLE-Arduino, INA219 |
| Serial logging | Structured log levels: DEBUG, INFO, WARN, ERROR |
| Config.h | All pin definitions and tuning constants |
| FreeRTOS task skeleton | Empty tasks for all subsystems |

**Dependencies:** None  
**Testing Criteria:**
- Firmware compiles without warnings
- All tasks start without stack overflow on boot

---

### B2 — Device Identity

| Task | Details |
|---|---|
| NVS initialization | `nvs_flash_init()`, namespace `identity` |
| Node ID generation | UUID v4, stored in NVS |
| ECDH key pair | mbedTLS P-256, stored in NVS |
| Identity load on boot | Read from NVS; generate only if absent |

**Dependencies:** B1  
**Testing Criteria:**
- Node ID is identical across reboots
- Key pair is stable across reboots

---

### B3 — LoRa Subsystem

| Task | Details |
|---|---|
| SX1278 initialization | Correct pin config, frequency, SF, BW, CR |
| DIO0 ISR | ISR reads packet into RX queue, posts to FreeRTOS queue |
| TX task | Queue-driven, encrypts and transmits, returns to RX mode |
| RX task | Dequeues raw bytes, deserializes, sends to mesh router |
| CAD implementation | Channel Activity Detection before TX |
| RSSI/SNR tagging | Tag each received packet with signal quality |

**Dependencies:** B2  
**Testing Criteria:**
- LoRa init success logged on boot
- TX packet visible on a second node's serial monitor
- RX packet received and deserialized correctly from a second node

---

### B4 — BLE GATT Server

| Task | Details |
|---|---|
| GATT service setup | Custom service UUID, TX/RX/Status characteristics |
| BLE advertisement | Advertise with Node ID in local name |
| Write callback | Receive app-sent packets, enqueue to LoRa TX queue |
| Notify mechanism | Send received LoRa packets to connected app |
| Status characteristic | Periodically update with power telemetry |
| Reconnection handling | Accept reconnections without restart |

**Dependencies:** B3  
**Testing Criteria:**
- Android app discovers and connects to ESP32 BLE advertisement
- BLE write from app triggers LoRa TX
- BLE notify delivers LoRa-received packet to app within 500 ms

---

### B5 — Mesh Routing and Store-and-Forward

| Task | Details |
|---|---|
| Seen-packet cache | Circular buffer, 128 entries |
| TTL management | Decrement and drop at TTL=0 |
| Routing logic | Forward / deliver / store decision tree |
| SPIFFS initialization | Mount SPIFFS for packet cache storage |
| Store-and-forward cache | Write pending packets to SPIFFS |
| Retry task | Periodic retry of cached packets |
| Cache cleanup | Delete expired or max-retry packets |

**Dependencies:** B4  
**Testing Criteria:**
- Packet from Node A reaches Node C via relay Node B (3-node test)
- Cached packet is delivered when offline destination node comes online
- Seen-packet cache prevents duplicate delivery

---

### B6 — Power Monitor and Optimization

| Task | Details |
|---|---|
| INA219 driver | I2C init, calibration, polling task |
| Power reading BLE notify | Push telemetry to Status characteristic |
| Battery saving mode | Enter light sleep when BLE disconnected; throttle INA polling |
| Power logging | Periodic SPIFFS log of telemetry |

**Dependencies:** B5  
**Testing Criteria:**
- INA219 readings appear in serial log every 5 seconds
- Android app displays live voltage and current
- ESP32 current drops to < 30 mA in light sleep (measured)

---

## Phase H — Hardware Development

**Goal:** A reproducible, physically reliable hardware assembly suitable for field deployment.

---

### H1 — Breadboard Prototype

| Task | Details |
|---|---|
| Component procurement | All components sourced and verified |
| Breadboard assembly | Per wiring guide |
| Antenna attachment | U.FL connection verified |
| Firmware flash | B1 firmware flashed and booting |
| Basic LoRa test | Two nodes communicate at 1 m range |

**Dependencies:** None  
**Testing Criteria:**
- Both nodes boot and initialize without errors
- LoRa packet exchange confirmed between two breadboard nodes

---

### H2 — Range and Signal Testing

| Task | Details |
|---|---|
| Open-field range test | Measure RSSI vs. distance at 100 m, 500 m, 1 km, 2 km |
| Urban range test | Test through buildings, vegetation |
| Antenna orientation test | Compare vertical vs. horizontal placement |
| Interference test | Check for 433 MHz interference in test environment |
| SF tuning | Test SF9 vs SF10 vs SF11 for range/data rate tradeoff |

**Dependencies:** H1  
**Testing Criteria:**
- Confirmed communication at ≥ 1 km line-of-sight
- RSSI data documented in a range table

---

### H3 — Power Validation

| Task | Details |
|---|---|
| INA219/226 inline calibration | Verify readings against known USB power meter |
| Active power measurement | Measure current in BLE+LoRa active mode |
| Sleep power measurement | Measure current in light sleep mode |
| Runtime estimation | Calculated vs. measured for test power bank |
| Power bank compatibility | Test with 3 different power bank models |

**Dependencies:** H2  
**Testing Criteria:**
- INA readings within 10% of reference USB power meter
- Active current < 200 mA
- Sleep current < 30 mA

---

### H4 — Permanent Build

| Task | Details |
|---|---|
| Perfboard layout | Design component placement |
| Soldering | Solder all connections |
| Mechanical strain relief | Hot glue U.FL connector, cable ties |
| Enclosure | 3D-printed or commercial weatherproof box |
| Label | Print component and pin labels on enclosure |

**Dependencies:** H3  
**Testing Criteria:**
- All subsystems pass functional test after soldering
- Enclosure fits all components with antenna accessible externally

---

## Integration Milestones

| Milestone | Phases Required | Criteria |
|---|---|---|
| Alpha: BLE + LoRa Bridge | A3 + B4 | App connects to ESP32 and transmits a packet over LoRa |
| Beta: Full Mesh Chat | A4 + B5 | Private and global chat working across 3 nodes |
| Emergency RC: SOS + Location | A5 + B5 + H2 | SOS packet traverses 2 hops, received and displayed |
| Field Ready v1.0 | All phases | All features, < 5 critical bugs, field test passed |
