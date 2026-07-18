# Testing Strategy

This document outlines the testing strategy for the Offline Emergency Mesh Communication System. It covers software validation, hardware tests, and integration verification.

---

## 1. Test Levels

The testing process is structured into four levels:

```
                  +--------------------------------+
                  |         Field Testing          |
                  |  (Range, Multi-node, Battery)  |
                  +---------------+----------------+
                                  ▲
                                  │
                  +---------------+----------------+
                  |      Integration Testing       |
                  |  (Android BLE, LoRa, Protocol) |
                  +---------------+----------------+
                                  ▲
                                  │
                  +---------------+----------------+
                  |       Simulation Testing       |
                  |  (50, 100, 1000 Node Meshes)   |
                  +---------------+----------------+
                                  ▲
                                  │
                  +---------------+----------------+
                  |         Unit/UI Tests          |
                  |   (Compose, Room, mbedTLS)     |
                  +--------------------------------+
```

---

## 2. Directory Index

Refer to the following testing documents for specific test protocols:

* [Simulation Testing](simulation-testing.md): Routing performance metrics, node simulation engines, and scalability tests (50, 100, 1000 nodes).
* [Field Testing](field-testing.md): Outdoor range testing, signal quality (RSSI/SNR), and power bank runtime measurements.

---

## 3. Test Scopes

### A. Unit Testing
* **Android**: Room DAOs, Use Cases, domain entities, and packet serialization utilities are tested using JUnit and Mockk.
* **ESP32**: Seen-cache circular buffers and packet routing decisions are tested via PlatformIO Native Unit testing.

### B. UI Testing
* Android UI interactions, including chat screen inputs and navigation paths, are validated using Compose UI Test APIs.

### C. Bluetooth Testing (Without LoRa)
* BLE tests verify device discovery, GATT services, connection states, and MTU negotiation without requiring active LoRa transmissions.

### D. Security Testing
* Validates E2E encryption tag errors (AEAD authentication tag failures), signature rejections (modified signatures), and replay detection time bounds.
