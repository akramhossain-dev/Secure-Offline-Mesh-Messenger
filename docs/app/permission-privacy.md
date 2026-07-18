# Permissions & Privacy

This document outlines the Android system permissions requested by the application and the privacy model protecting user telemetry and location metrics in field conditions.

---

## 1. System Permissions Reference

The application operates without internet access, greatly reducing security vulnerabilities. However, access to local radios, audio hardware, and system alerts requires explicit runtime permissions.

| Permission | Purpose | Requested When | Enforcement Level |
|---|---|---|---|
| `BLUETOOTH_SCAN` | Search for nearby paired or floating ESP32 nodes | Scanning page / BLE setup | Mandatory for operation |
| `BLUETOOTH_CONNECT` | Establish GATT connection and write/read characteristics | Background connection | Mandatory for operation |
| `BLUETOOTH_ADVERTISE` | Enable nearby users to discover this device via advertising payload | Pairing screen / Profile setup | User-configurable |
| `ACCESS_FINE_LOCATION` | Fetch high-precision GPS coordinates for SOS and map display | Tapping map / Triggering SOS | Required for location features |
| `RECORD_AUDIO` | Access local microphone hardware to capture voice clips | Pressing voice record | Required for Voice Chat |
| `POST_NOTIFICATIONS` | Deliver incoming chat indicators and system warning status alerts | App background / First boot | Optional but recommended |

---

## 2. Location Privacy Protocol

Location data is highly sensitive during emergency scenarios. The system strictly isolates and controls GPS access.

### A. Explicit User-Triggered Sharing
* The application **never** transmits coordinates autonomously in the background during normal operations.
* Coordinates are polled from Android's `FusedLocationProviderClient` only when the user explicitly triggers sharing.
* Location shares are configured with self-expiring lifetimes (15 minutes, 1 hour, or until stopped). Once expired, local records are flagged inactive and further transmissions are blocked.

### B. SOS Location Flow
* Triggering an SOS overrides background restrictions using a **Foreground Service** to maintain access to GPS coordinates.
* The system displays a high-visibility persistent notification in the status bar while SOS mode is active.
* Coordinates are refreshed and broadcast every 60 seconds.
* Canceling the SOS immediately stops location polling and halts background tasks.

---

## 3. User Privacy Controls

### A. BLE Advertisement Toggle (Public vs. Private)
Users can change their network visibility within the Settings screen:
* **Public**: The device advertises its Node ID via Bluetooth BLE, allowing nearby devices to detect and pair with it.
* **Private**: BLE advertising is disabled. The device does not broadcast advertisements but can still receive mesh routing traffic and listen to incoming notifications.

### B. Ephemeral Data Store
* Private messages are encrypted end-to-end (E2E) using AES-256-GCM. Unpaired intermediate nodes cannot decrypt payloads.
* Global Chat and presence HELLO packets are intentionally transmitted in plaintext to allow open discovery and public coordination. Users are explicitly warned about this before composing global chats.
* Local databases can be wiped instantly via the "Clear All Data" button in Settings, executing an encrypted rewrite cycle over local storage before deletion.
