# Release Preparation Guide
**Offline Emergency Mesh Communication System — Phase A35**

## 1. Installation Guide

### Prerequisites
- Android device running Android 8.0 (API level 26) or higher.
- Bluetooth Low Energy (BLE) compatible transceiver.
- Mock location disabled (unless simulating coordinates).

### Steps to Install APK
1. Download the release APK (`app-release.apk`) to your device.
2. Open Settings on your device, navigate to **Apps & Notifications** -> **Special App Access** -> **Install Unknown Apps** and grant permission for your browser or file manager.
3. Open the APK file and select **Install**.

---

## 2. User Guide

### 1. Account Initialization
- Open the app. You will be greeted with the setup wizard.
- Enter a nickname (e.g., `Alice`). Your cryptographic identity keys (ECDH `secp256r1`) are automatically generated and stored inside the Android KeyStore.

### 2. Discovered Nodes (BLE Peer List)
- Navigate to the **Contacts / Node Map** tab.
- Click **Scan** to search for nearby mesh router nodes or other offline users.
- Tap a node to inspect RSSI signal levels, battery levels, and capabilities (such as Relay Capability).

### 3. Messaging
- Select a peer from the list.
- Send text or voice messages. All communication is automatically encrypted with AES-256-GCM.
- Message status indicators:
  - `Queued` (Yellow dot): Waiting in the offline sync queue.
  - `Sent` (Single check): Broadcasted to the mesh.
  - `Delivered` (Double check): Acknowledged by the recipient.

---

## 3. Developer Guide

### Project Setup
1. Clone the repository.
2. Copy `local.properties.template` to `local.properties` and configure `sdk.dir`.
3. Open the project in Android Studio.

### Building
- **Debug Build:** Run the `:app:assembleDebug` gradle task. Includes verbose logger stubs and mock locations.
- **Release Build:** Set up release keystore variables inside `local.properties` and run `:app:assembleRelease`.

---

## 4. Architecture Documentation

The system is split into three clean architecture layers:
1. **Core / Domain:** Holds data contracts, business rules (use cases), and interfaces (`KeyManager`, `MapRepository`, etc.).
2. **Data:** Handles persistent cache logic (Room databases, DAOs, Entity tables) and background execution managers (WorkManager instances).
3. **UI / Presentation:** Built entirely in Jetpack Compose utilizing MVVM + MVI pattern.

```
┌────────────────────────────────────────────────────────┐
│                        UI Layer                        │
│             (Jetpack Compose, MVVM ViewModels)         │
└───────────────────────────┬────────────────────────────┘
                            │ Calls Use Cases
                            ▼
┌────────────────────────────────────────────────────────┐
│                       Domain Layer                     │
│                  (Use Cases & Repository Contracts)     │
└───────────────────────────┬────────────────────────────┘
                            │ Implements Interfaces
                            ▼
┌────────────────────────────────────────────────────────┐
│                        Data Layer                      │
│            (Room DB, KeyStore, CommunicationManager)   │
└────────────────────────────────────────────────────────┘
```

---

## 5. Release Assets & Notes

### Version Information
- **Version Code:** `1`
- **Version Name:** `1.0.0-gold`

### Release Notes (v1.0.0)
- **Decentralized Map Engine:** Added canvas maps loaded entirely offline with custom layers support.
- **Decentralized Sync Queue:** Holds up to 500 queued operations for deferred delivery.
- **Battery Saver Gating:** Automatic throttle adjustments based on 4 battery modes.
- **Key Rotation Support:** Rotate identity keys directly from the settings drawer.
