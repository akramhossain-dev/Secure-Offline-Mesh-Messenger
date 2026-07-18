# Power System

This document outlines the power system architecture of the mesh node, detailing telemetry, runtime metrics, and optimizations.

---

## 1. Power Bank Source

The hardware relies on a commercial USB power bank (5V output) as its power supply. This eliminates the need for custom battery chargers (like TP4056 modules) or loose Li-ion batteries in the field.
* **Input Voltage**: 5.0 V via the micro-USB or USB-C port of the ESP32 Dev Board.
* **On-board LDO**: The ESP32's internal AMS1117 regulator steps this 5.0V input down to a stable 3.3V for both the ESP32 and the SX1278 transceiver.

---

## 2. Telemetry (INA219 Current Sensor)

The INA219 sensor is used to monitor power telemetry. It is connected to the ESP32 via the I2C bus:

### A. Telemetry Wiring

| INA219 Pin | ESP32 GPIO | Purpose |
|---|---|---|
| **VCC** | 3.3V | Sensor supply voltage. |
| **GND** | GND | Common ground reference. |
| **SDA** | GPIO21 | I2C Data line. |
| **SCL** | GPIO22 | I2C Clock line. |
| **VIN+** | Power Bank (+) | Positive power input before the shunt resistor. |
| **VIN-** | ESP32 5V Input | Output after the inline shunt resistor. |

### B. Shunt Resistor Calibration
* A 0.1 Ω, 1% precision shunt resistor is placed in series along the positive rail.
* The INA219 registers the small voltage drop across this shunt to compute current draw in milliamperes.

---

## 3. Node Power Consumption States

Average current metrics measured at 5.0 V input:

| State | ESP32 Configuration | LoRa State | Current Draw |
|---|---|---|---|
| **Active BLE & LoRa** | Both cores active, Bluetooth connected. | RX continuous listening. | ~160 mA |
| **Active LoRa Only** | Core 0 idle, BLE disconnected. | RX continuous listening. | ~90 mA |
| **Power Saving** | Core 0 light sleep, BLE advertising reduced. | RX periodic wake. | ~20 mA |
| **TX Transmit Burst** | Active transmission. | RF TX output (+17 dBm). | ~240 mA (Peak) |

---

## 4. Light Sleep Optimizations

When no BLE companion device is connected, the firmware enters a low-power duty cycle:
* **ESP32 Core Sleep**: The CPU enters light sleep between LoRa receiver intervals, dropping current draw to ~20 mA.
* **Throttled Telemetry**: The INA219 polling task interval is extended from 5 seconds to 30 seconds to conserve battery.
