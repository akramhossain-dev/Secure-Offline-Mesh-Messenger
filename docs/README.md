# Offline Emergency Mesh Communication System

> A decentralized, infrastructure-free communication platform for emergency and remote scenarios.

---

## What This System Does

This system enables two-way text, voice, and emergency messaging between Android devices — with **no internet, no SIM card, and no Wi-Fi required**.

The application supports **two communication methods** managed automatically by the Communication Manager:

| Method | When Used | Range |
|---|---|---|
| **Bluetooth BLE** | Receiver is a nearby device | Up to ~10 m |
| **LoRa 433MHz Mesh** | Receiver is far away or across the mesh | 1–5 km per hop |

The Communication Manager selects the best available transport transparently. Users never choose manually. When the receiver is offline, messages are queued via **Store & Forward** and delivered when the destination node reappears. Every ESP32 node participates in mesh routing, enabling multi-hop delivery across large distances.

---

## When To Use It

| Scenario | Capability |
|---|---|
| Natural disasters | Coordinate rescue, share location, broadcast SOS |
| Network shutdowns | Communicate independently of carrier infrastructure |
| Remote field operations | Long-range mesh coverage without repeaters |
| Search and rescue | Real-time coordination between teams |
| Off-grid communities | Persistent local communication network |

---

## System Components

| Component | Role |
|---|---|
| Android App | User interface, message composition, BLE bridge |
| ESP32 V1.3 (CH340C) | Network node, BLE host, LoRa controller |
| SX1278 RA-02 (433MHz) | Long-range RF transceiver |
| 433MHz Rubber Duck SMA Antenna | Better signal stability and extended range |
| U.FL/IPEX to SMA Adapter Cable | Connects RA-02 U.FL connector to external SMA antenna |
| INA219 | Voltage, current, and power monitoring |
| Power Bank | Portable power supply via USB |

---

## Documentation Index

### Product & Overview
* [Product Requirements](product/product-requirements.md): System goals, objectives, and NFRs.
* [Use Cases](product/use-cases.md): Actor flows and functional scenarios.
* [User Flow](product/user-flow.md): Flowcharts of normal and emergency states.
* [Project Overview](overview/project-overview.md): High-level problem statement and targets.
* [Features List](overview/features.md): Grouped app capabilities.
* [System Architecture](overview/system-architecture.md): Top-level block diagrams.

### Android Application Layer
* [App Overview](app/app-overview.md): Technical stack and design choices.
* [App Architecture](app/app-architecture.md): MVVM & Clean Architecture packages.
* [Screen Documentation](app/screen-documentation.md): Detailed page layout specifications.
* [Database Schema](app/database-schema.md): Room DB entity attributes and relation details.
* [State Management](app/state-management.md): Unidirectional data flows (UDF) and Flow configurations.
* [Permissions & Privacy](app/permission-privacy.md): System permissions check matrices.
* [Database Design](app/database-design.md): Entity Relationships and SQL types.
* [App Features](app/app-features.md): Specific UI implementations.

### Communications Layer
* [Communication Overview](communication/communication-overview.md): Multi-tier topology configurations.
* [Bluetooth Transport](communication/bluetooth-transport.md): NimBLE setup and GATT specifications.
* [LoRa Transport](communication/lora-transport.md): Semtech parameter matrices.
* [Hybrid Communication](communication/hybrid-communication.md): Transport selection flowchart.
* [Message Protocol](communication/message-protocol.md): Checksum validation and route decisions.
* [Packet Structure](communication/packet-structure.md): JSON envelope field mappings.
* [Store & Forward System](communication/store-forward-system.md): Background workers and retirement rules.
* [Communication Manager](app/communication-manager.md): App state controller details.
* [Transport Layer Interfaces](app/transport-layer.md): Transport API contracts.
* [Packet Protocol Spec](../firmware/packet-protocol.md): Serialized payload limits.

### Cryptography & Security
* [Security Overview](security/security-overview.md): Primary threat vectors and mitigation policies.
* [Encryption](security/encryption.md): E2E AES-GCM and mbedTLS specifics.
* [Identity Management](security/identity-management.md): Key generations and secure pair flows.
* [Privacy Model](security/privacy-model.md): Location polling triggers and scopes.
* [Security Design](security/security-design.md): Android Keystore security boundaries.

### Hardware & Firmware
* [Hardware Overview](hardware/hardware-overview.md): Base component references.
* [ESP32 Setup](hardware/esp32-setup.md): PlatformIO dependencies and task system.
* [LoRa Module](hardware/lora-module.md): SPI pin tables and antenna notes.
* [Power System](hardware/power-system.md): INA219 shunt metrics and sleep profiles.
* [Hardware Integration](hardware/hardware-integration.md): Breadboard layouts and integration checklists.
* [Components Reference](hardware/components.md): Datasheets and cost details.
* [Wiring Guide](hardware/wiring-guide.md): Connection schematics.
* [Setup Guide](hardware/setup-guide.md): Step-by-step soldering instructions.
* [ESP32 Architecture](../firmware/esp32-architecture.md): FreeRTOS core configurations.
* [LoRa Communication Spec](../firmware/lora-communication.md): Signal parameters.

### Testing & Deployment
* [Testing Strategy](testing/testing-strategy.md): Code unit tests and scope matrices.
* [Simulation Testing](testing/simulation-testing.md): Scaling matrix parameters (50, 100, 1000 nodes).
* [Field Testing](testing/field-testing.md): Real-world range walks and battery telemetry checks.
* [Roadmap Index](development/roadmap.md): General timelines.
* [Testing Protocols](development/testing.md): Automated and manual checklists.

### Roadmap
* [Development Roadmap](roadmap/development-roadmap.md): Phase sequence layouts.
* [Version Plan](roadmap/version-plan.md): Milestone scopes (v0.1 to v1.0).
* [Future Improvements](roadmap/future-improvements.md): Long-term system updates.

---

## Quick Hardware Reference

```
Power Bank → USB → ESP32 (3.3V pin) → SX1278 LoRa Module
```

| SX1278 Pin | ESP32 GPIO |
|---|---|
| VCC | 3.3V |
| GND | GND |
| SCK | GPIO18 |
| MISO | GPIO19 |
| MOSI | GPIO23 |
| NSS | GPIO5 |
| RST | GPIO14 |
| DIO0 | GPIO26 |

---

## Repository Structure

```
docs/
├── README.md
├── product/
│   ├── product-requirements.md
│   ├── use-cases.md
│   └── user-flow.md
├── app/
│   ├── app-overview.md
│   ├── app-architecture.md
│   ├── screen-documentation.md
│   ├── database-schema.md
│   ├── state-management.md
│   ├── permission-privacy.md
│   └── database-design.md
├── communication/
│   ├── communication-overview.md
│   ├── bluetooth-transport.md
│   ├── lora-transport.md
│   ├── hybrid-communication.md
│   ├── message-protocol.md
│   ├── packet-structure.md
│   └── store-forward-system.md
├── security/
│   ├── security-overview.md
│   ├── encryption.md
│   ├── identity-management.md
│   ├── privacy-model.md
│   └── security-design.md
├── hardware/
│   ├── hardware-overview.md
│   ├── esp32-setup.md
│   ├── lora-module.md
│   ├── power-system.md
│   ├── hardware-integration.md
│   ├── components.md
│   ├── wiring-guide.md
│   └── setup-guide.md
├── testing/
│   ├── testing-strategy.md
│   ├── simulation-testing.md
│   ├── field-testing.md
│   └── testing.md
└── roadmap/
    ├── development-roadmap.md
    ├── version-plan.md
    └── future-improvements.md
```

---

## License

This project is open-source and intended for humanitarian and emergency preparedness use.
