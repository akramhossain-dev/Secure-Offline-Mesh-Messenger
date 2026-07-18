# Hardware Integration

This document outlines the visual layout, breadboard column mapping, and verification checklist for integrating components into a single prototype node.

---

## 1. Prototype Layout Diagram

```
                 +-----------------------------------------+
                 |            830-Point Breadboard         |
                 +-----------------------------------------+
                 | [A1-A30] ESP32 Left Pin Rows            |
                 |                                         |
 [Power Bank]    | [J1-J30] ESP32 Right Pin Rows           |
      │          |                                         |
      ▼ (USB)    | [A32-A40] SX1278 RA-02 LoRa Module      |
 [INA219 INLINE] |                                         |
                 | [A42-A48] INA219 Current Sensor         |
                 +-----------------------------------------+
                                      │
                                      ▼ (U.FL IPEX Cable)
                                [Coaxial Adapter]
                                      │
                                      ▼ (SMA Thread)
                                [Rubber Duck Antenna]
```

---

## 2. Column Mapping Reference

For prototyping on an 830-point breadboard, the following column assignments are recommended to prevent wiring conflicts:

| Column Range | Target Component | Description |
|---|---|---|
| **A1 to A30** | ESP32 Board (Left Pins) | Handles SPI SCK/MISO/MOSI, NSS, and Reset signals. |
| **J1 to J30** | ESP32 Board (Right Pins) | Handles I2C SDA/SCL, power rails, and DIO0. |
| **A32 to A40** | SX1278 RA-02 Module | Mounted separately. Uses SPI jumpers back to the ESP32. |
| **A42 to A48** | INA219 Telemetry Board | Mounted below LoRa. Connects to I2C lines. |
| **Left Bus Rail** | 3.3V / GND distribution | Distributes LDO output from the ESP32 to both sensor and LoRa. |

---

## 3. Visual Checklist Before Power-On

Before plugging in the USB power bank, verify the following:

- [ ] **Antenna Safety**: The Rubber Duck SMA antenna is attached securely to the IPEX adapter cable, and the U.FL connector is clicked into the RA-02 module.
- [ ] **Power Rails**: Ensure the SX1278 is wired to the **3.3V** rail, not the 5.0V input rail.
- [ ] **No Pin Shorts**: Check for loose wires or pin bridges between adjacent ESP32 pins.
- [ ] **I2C Address**: The address pins on the INA219 are left floating or tied to GND to ensure it operates at address `0x40`.
- [ ] **Common Ground**: Verify that the ground pins of the ESP32, SX1278, and INA219 are connected to the same ground bus.
