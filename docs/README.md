# Offline Emergency Mesh Communication System

> A decentralized, infrastructure-free communication platform for emergency and remote scenarios.

---

## What This System Does

This system enables two-way text, voice, and emergency messaging between Android devices through an ESP32 + SX1278 LoRa hardware node — with **no internet, no SIM card, and no Wi-Fi required**.

Communication flows over a **LoRa 433MHz mesh network**, bridged to Android phones via **Bluetooth BLE**. Every node participates in routing, enabling multi-hop message delivery across large distances.

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
| 433MHz Spring Antenna | Signal transmission and reception |
| INA219/INA226 | Voltage, current, and power monitoring |
| Power Bank | Portable power supply via USB |

---

## Documentation Index

| Section | Description |
|---|---|
| [Project Overview](overview/project-overview.md) | Goals, use cases, design principles |
| [System Architecture](overview/system-architecture.md) | Full system architecture with diagrams |
| [Features](overview/features.md) | Complete feature list |
| [App Architecture](app/app-architecture.md) | Android MVVM + Clean Architecture breakdown |
| [App Features](app/app-features.md) | UI, UX, and application feature details |
| [Database Design](app/database-design.md) | Room Database schema and relationships |
| [ESP32 Architecture](firmware/esp32-architecture.md) | Firmware structure and task system |
| [LoRa Communication](firmware/lora-communication.md) | SX1278 configuration, range, and operation |
| [Packet Protocol](firmware/packet-protocol.md) | Message format, packet types, and routing |
| [Components](hardware/components.md) | Hardware specifications and datasheets |
| [Wiring Guide](hardware/wiring-guide.md) | SPI wiring, pinout, and connection table |
| [Setup Guide](hardware/setup-guide.md) | Assembly, breadboard, and prototype steps |
| [Security Design](security/security-design.md) | Encryption, key exchange, and authentication |
| [Roadmap](development/roadmap.md) | Development phases A, B, H |
| [Testing](development/testing.md) | Software, hardware, and field testing |

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
├── overview/
│   ├── project-overview.md
│   ├── system-architecture.md
│   └── features.md
├── app/
│   ├── app-architecture.md
│   ├── app-features.md
│   └── database-design.md
├── firmware/
│   ├── esp32-architecture.md
│   ├── lora-communication.md
│   └── packet-protocol.md
├── hardware/
│   ├── components.md
│   ├── wiring-guide.md
│   └── setup-guide.md
├── security/
│   └── security-design.md
└── development/
    ├── roadmap.md
    └── testing.md
```

---

## License

This project is open-source and intended for humanitarian and emergency preparedness use.
