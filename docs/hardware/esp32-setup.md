# ESP32 Setup

This document describes the firmware environment setup and core configurations for the ESP32 V1.3 Dev Board.

---

## 1. Development Environment

The firmware is developed using **PlatformIO** (VS Code Plugin) over the **Arduino Framework**:

### Configuration (`platformio.ini`)
```ini
[env:esp32dev]
platform = espressif32
board = esp32dev
framework = arduino
monitor_speed = 115200
upload_speed = 921600
lib_deps =
    sandeepmistry/LoRa @ ^0.8.0
    bblanchon/ArduinoJson @ ^7.0.0
    h2zero/NimBLE-Arduino @ ^1.4.1
    adafruit/Adafruit INA219 @ ^1.2.3
build_flags =
    -D CORE_DEBUG_LEVEL=3
    -D CONFIG_ARDUHAL_LOG_COLORS=1
```

---

## 2. NimBLE Bluetooth Config

The firmware utilizes the **NimBLE-Arduino** library instead of the default Bluedroid BLE stack:
* **Memory Optimization**: NimBLE reduces RAM utilization by over 50% and flash footprint by 100KB+.
* **Pairing and Reconnections**: Provides reliable reconnections with less overhead during background scans.

---

## 3. Dual-Core Task System

The ESP32 dual-core Xtensa processor handles operations concurrently using FreeRTOS tasks:

| Core | Task Name | Priority | Role |
|---|---|---|---|
| **Core 0** | `TaskBleNotify` | 7 | BLE GATT callbacks and notifications. |
| **Core 0** | `TaskPowerMonitor` | 2 | Periodic (5s) polling of the INA219 sensor. |
| **Core 1** | `TaskLoRaRx` | 10 | ISR-triggered receiver queue processing (Highest Priority). |
| **Core 1** | `TaskLoRaTx` | 9 | Non-blocking transmission queue driver. |
| **Core 1** | `TaskMeshRouter` | 8 | Seen cache checks and relay decisions. |
| **Core 1** | `TaskStoreForward` | 3 | SPIFFS-backed periodic queue retry. |

---

## 4. Initial Diagnostics Check

Upon booting, the ESP32 performs a diagnostics check, outputting results over Serial at `115200` baud:
* **NVS Storage**: Loads the persistent Node UUID.
* **SPI Bus Initialization**: Verifies connection to the SX1278 transceiver. If fails, outputs `[LORA] init failed!`.
* **I2C Bus Initialization**: Verifies the INA219 address at `0x40`.
* **BLE Advertisement**: Verifies the NimBLE stack is active and advertising `MeshNode-<shortID>`.
