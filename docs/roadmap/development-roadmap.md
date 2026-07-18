# Development Roadmap

This document outlines the development order and timeline for building the Offline Emergency Mesh Communication System.

---

## 1. Development Lifecycle Phases

The project is structured into seven sequential phases, ensuring software architecture is finalized and validated before purchasing hardware:

```
+------------------------------------------+
|  Phase A: Android Application Foundation  |
+--------------------+---------------------+
                     │
                     ▼
+------------------------------------------+
|      Phase B: ESP32 Firmware Design      |
+--------------------+---------------------+
                     │
                     ▼
+------------------------------------------+
|     Phase C: Communication Protocol      |
+--------------------+---------------------+
                     │
                     ▼
+------------------------------------------+
|            Phase D: Security             |
+--------------------+---------------------+
                     │
                     ▼
+------------------------------------------+
|       Phase H: Hardware Assembly         |
|   (Gate: Sourced after A & B complete)   |
+--------------------+---------------------+
                     │
                     ▼
+------------------------------------------+
|           Phase I: Integration           |
+--------------------+---------------------+
                     │
                     ▼
+------------------------------------------+
|            Phase T: Testing              |
+------------------------------------------+
```

---

## 2. Phase Details

### Phase A: Android Application
* **Tasks**: Project setup, MVVM core architecture, DI configuration, database schema, and navigation setup.
* **Gate**: Validated UI navigation flows and database creations.

### Phase B: ESP32 Firmware
* **Tasks**: PlatformIO project configuration, FreeRTOS task structures, and NimBLE BLE configurations.
* **Gate**: Clean builds and successful boots on simulated hardware.

### Phase C: Communication Protocol
* **Tasks**: JSON packet structures, seen-packet cache implementation, and TTL routing logic.

### Phase D: Security
* **Tasks**: X25519 key exchange derivation, AES-256-GCM encryption integrations, and HMAC signatures.

### Phase H: Hardware Assembly
* **Tasks**: Sourcing parts, wiring guides, breadboard prototyping, and telemetry sensor hookups.
* **Prerequisite**: Sourced only after Phase A and Phase B software architectures are finalized.

### Phase I: Integration
* **Tasks**: End-to-end integration: Android BLE connectivity, LoRa transmissions, and Store & Forward delivery.

### Phase T: Testing
* **Tasks**: Unit tests, integration tests, range validation, and power consumption profiles.
