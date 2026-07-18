# Hardware Overview

This document describes the hardware architecture of the Offline Emergency Mesh Communication System. The hardware is designed to be affordable, accessible, and simple to assemble.

---

## 1. Complete Component List

Each mesh node requires the following components:

| # | Component | Model / Specification | Purpose |
|---|---|---|---|
| **1** | ESP32 Board | ESP32 V1.3 Dev Board (CH340C USB-Serial) | Central MCU, BLE controller, SPI master. |
| **2** | LoRa Transceiver | SX1278 RA-02 433MHz AI-Thinker Module | Long-range RF transceiver. |
| **3** | SMA Antenna | 433MHz Rubber Duck SMA Antenna (50 Ω) | Improved gain and signal stability. |
| **4** | Coaxial Cable | U.FL / IPEX to SMA Female Adapter Cable | Connects RA-02 U.FL to SMA antenna. |
| **5** | Telemetry Sensor | INA219 Current Sensor (I2C) | Real-time voltage, current, and power monitoring. |
| **6** | Power Source | USB Power Bank (5V output) | Portability power source. |

---

## 2. Directory Index

Refer to the following sub-documents for detailed hardware guides:

* [ESP32 Setup](esp32-setup.md): PlatformIO configuration, flashing steps, and task layout.
* [LoRa Module](lora-module.md): Semtech SX1278 transceiver parameters, range expectations, and wiring logic.
* [Power System](power-system.md): Power Bank estimates, INA219 current sensing calibrations, and light sleep optimizations.
* [Hardware Integration](hardware-integration.md): Step-by-step schematic layout and breadboard column assignments.

---

## 3. Simplified Circuit Interconnection

```
[Power Bank 5V] 
       │
       ▼ (USB Cable)
[INA219 inline monitor]
       │
       ▼ (5V USB Pin)
[ESP32 Development Board (3.3V LDO)]
       │
       ├─ (I2C SDA/SCL) ─────► [INA219 Telemetry]
       │
       └─ (SPI SCK/MISO/MOSI) ► [SX1278 RA-02 LoRa Module]
                                       │
                                       ▼ (U.FL IPEX Cable)
                                [Rubber Duck Antenna]
```

* **No GPS module is present on the hardware**.
* **No physical panic button is present on the hardware**.
* **No custom battery-charging circuits (TP4056 or Li-ion components) are built on the hardware**. All power is managed via standard, commercial USB power banks.
