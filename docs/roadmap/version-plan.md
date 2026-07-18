# Version Plan

This document details the feature releases for the project, tracking progress from initial setup to a field-ready release.

---

## 1. Release Milestone Map

```
    v0.1 (Android Foundation)
             │
             ▼
    v0.2 (Bluetooth Communication)
             │
             ▼
    v0.3 (Mock LoRa Integration)
             │
             ▼
    v0.4 (Real ESP32 + LoRa Link)
             │
             ▼
    v0.5 (Mesh Routing Engine)
             │
             ▼
    v1.0 (Emergency Comms Platform)
```

---

## 2. Release Milestones

### A. v0.1: Android Foundation
* **Goal**: Establish the base codebase.
* **Scope**:
  * Set up Gradle dependencies (Room, Hilt, Compose, Navigation).
  * Build the initial Room database schema.
  * Implement onboarding and profile creation screens.

### B. v0.2: Bluetooth Communication
* **Goal**: Enable direct BLE communication between the Android app and the ESP32 node.
* **Scope**:
  * Set up NimBLE GATT Server on the ESP32.
  * Implement connection state tracking and reconnection logic in the Android app.
  * Verify direct message transmission over BLE.

### C. v0.3: Mock LoRa Integration
* **Goal**: Test the message protocol without requiring physical LoRa transceivers.
* **Scope**:
  * Implement a mock LoRa transport class on both the Android app and ESP32.
  * Verify packet serialization, seen cache checks, and signature validations.

### D. v0.4: Real ESP32 + LoRa Link
* **Goal**: Establish a physical wireless link between two nodes.
* **Scope**:
  * Integrate the Semtech SX1278 transceiver with the ESP32 over SPI.
  * Test point-to-point LoRa transmissions.
  * Measure RSSI/SNR signal parameters.

### E. v0.5: Mesh Routing Engine
* **Goal**: Implement multi-hop packet routing and Store & Forward capabilities.
* **Scope**:
  * Implement TTL decrement logic and routing decisions.
  * Set up local Store & Forward storage in the Room database.
  * Test message relays across a 3-node physical layout.

### F. v1.0: Emergency Communication Platform
* **Goal**: A field-ready release suitable for deployment.
* **Scope**:
  * Implement SOS broadcasting and offline map integration.
  * Implement E2E encryption and key exchanges.
  * Enable the Network Dashboard and INA219 power telemetry.
  * Conduct outdoor range and power validation tests.
