# App Overview

**Platform:** Android  
**Language:** Kotlin  
**Minimum SDK:** 26 (Android 8.0)  
**Target SDK:** 34 (Android 14)  
**UI Framework:** Jetpack Compose  
**Architecture:** MVVM + Clean Architecture  

---

## What the App Does

The Android application is the user-facing component of the Offline Emergency Mesh Communication System. It provides:

- A messaging interface for private and global communication
- Emergency features: SOS, emergency status, location sharing
- An offline map showing contacts and emergency markers
- A network dashboard for monitoring the mesh
- A resource sharing board for coordinating logistics

All of this functions without internet, cellular service, or Wi-Fi.

---

## How It Connects to the Mesh

The Android app does not communicate directly with other Android devices. It acts as a **controller** for a paired **ESP32 hardware node**, using Bluetooth BLE:

```
Android App
    │
    │  Bluetooth BLE (GATT)
    ▼
ESP32 Node
    │
    │  SPI
    ▼
SX1278 LoRa Module
    │
    │  433MHz RF
    ▼
LoRa Mesh Network
```

The app sends outbound packets to the ESP32 via BLE GATT Write, and receives inbound packets via BLE Notify. The ESP32 handles all LoRa radio communication.

For **nearby devices** within BLE range, the app can communicate directly without the LoRa hop.

---

## Technology Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose |
| Architecture | MVVM + Clean Architecture |
| Dependency Injection | Hilt |
| Database | Room (SQLite) |
| Concurrency | Kotlin Coroutines + Flow |
| Background Work | WorkManager |
| Location | FusedLocationProviderClient |
| Camera (QR) | CameraX + ZXing |
| Audio | MediaRecorder / MediaPlayer |
| Offline Map | OsmDroid (OpenStreetMap tiles) |
| Encryption | Android Keystore + javax.crypto |
| BLE | Android Bluetooth LE API |
| Navigation | Jetpack Navigation Compose |

---

## Key Design Decisions

### Offline-First
All data is stored locally in Room Database. The app functions fully without any connectivity. No remote API calls are made at any point.

### No Manual Transport Selection
The Communication Manager automatically selects Bluetooth, LoRa, or Store & Forward based on receiver reachability. Users compose messages normally; routing is invisible.

### Clean Architecture Enforcement
The UI layer has no knowledge of the data layer. Use Cases are the only bridge between ViewModels and repositories. This makes testing straightforward and the codebase maintainable.

### Privacy by Default
The app requests only the permissions it needs, precisely when they are needed. Location access is never taken in the background. BLE advertising is user-controllable.

---

## Application Modules

| Module | Responsibility |
|---|---|
| `presentation` | Compose screens, ViewModels, navigation |
| `domain` | Use cases, domain models, repository interfaces |
| `data/local` | Room Database, DAOs, entity mappings |
| `data/communication` | Communication Manager, Bluetooth & LoRa transports, Store & Forward |
| `data/crypto` | AES-256-GCM encryption, ECDH key exchange, Android Keystore |
| `data/repository` | Repository implementations |
| `di` | Hilt modules |
| `util` | QR generation, GPS helper, audio encoder |

---

## Related Documents

- [App Architecture](app-architecture.md) — Detailed MVVM + Clean Architecture design
- [Screen Documentation](screen-documentation.md) — Every screen documented
- [State Management](state-management.md) — StateFlow and UI state patterns
- [Database Schema](database-schema.md) — Full Room Database schema
- [Permission & Privacy](permission-privacy.md) — Android permission handling
- [Communication Manager](communication-manager.md) — Transport selection logic
