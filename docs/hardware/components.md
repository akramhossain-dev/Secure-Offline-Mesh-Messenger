# Hardware Components

---

## Component List

| # | Component | Specification | Purpose |
|---|---|---|---|
| 1 | ESP32 Dev Board | V1.3, CH340C, NodeMCU-32S | Main controller, BLE host, SPI master |
| 2 | SX1278 LoRa Module | RA-02, 433 MHz, Original AI Thinker | Long-range RF transceiver |
| 3 | 433MHz Rubber Duck SMA Antenna | 433 MHz, SMA connector, 50 Ω | Better signal stability and extended range |
| 4 | U.FL/IPEX to SMA Adapter Cable | Connects RA-02 U.FL to SMA antenna | External antenna support |
| 5 | INA219 | Current sensor, I2C | Power monitoring |
| 6 | Power Bank | 5V USB output, ≥ 5000 mAh | Power supply |
| 7 | USB Cable | Micro-USB or USB-C | Power delivery to ESP32 |
| 8 | Jumper Wires | Male-to-Male, Male-to-Female | Breadboard connections |
| 9 | Breadboard | 830-point | Prototype assembly |

---

## ESP32 V1.3 Dev Board (CH340C)

| Specification | Value |
|---|---|
| Chipset | Espressif ESP32-WROOM-32 |
| CPU | Dual-core Xtensa LX6, 240 MHz |
| RAM | 520 KB SRAM |
| Flash | 4 MB (default) |
| USB-to-Serial | CH340C |
| Operating Voltage | 3.3 V (logic) |
| Input Voltage (USB) | 5 V |
| GPIO Voltage | 3.3 V |
| Bluetooth | BLE 4.2 + Classic (BLE used in this project) |
| Wi-Fi | 802.11 b/g/n (not used in this project) |
| SPI | Hardware SPI, up to 80 MHz |
| I2C | Hardware I2C, configurable pins |
| UART | 3× hardware UART |
| Operating Temperature | –40°C to +85°C |
| Form Factor | 38-pin DIP, breadboard-compatible |

### Pin Summary (Used in This Project)

| GPIO | Function | Connected To |
|---|---|---|
| 3.3V | Power output | SX1278 VCC |
| GND | Ground | SX1278 GND, INA GND |
| GPIO18 | SPI SCK | SX1278 SCK |
| GPIO19 | SPI MISO | SX1278 MISO |
| GPIO23 | SPI MOSI | SX1278 MOSI |
| GPIO5 | SPI NSS | SX1278 NSS |
| GPIO14 | LoRa Reset | SX1278 RST |
| GPIO26 | LoRa DIO0 IRQ | SX1278 DIO0 |
| GPIO21 | I2C SDA | INA SDA |
| GPIO22 | I2C SCL | INA SCL |

---

## SX1278 RA-02 LoRa Module

| Specification | Value |
|---|---|
| Chipset | Semtech SX1278 |
| Manufacturer Module | AI Thinker RA-02 |
| Frequency | 433 MHz |
| Modulation | LoRa (CSS) |
| Max Output Power | +20 dBm |
| Receiver Sensitivity | –148 dBm (max SF, max BW) |
| Configured Sensitivity | –137 dBm (SF10, BW125) |
| Supply Voltage | 3.3 V |
| TX Current | 120 mA (at +20 dBm) |
| RX Current | 10.8 mA |
| Sleep Current | 0.2 µA |
| Interface | SPI (Mode 0) |
| Antenna Connector | IPX / U.FL |
| Dimensions | 16 × 16 mm |
| Operating Temperature | –40°C to +85°C |

### SX1278 Pin Description

| Pin | Name | Direction | Description |
|---|---|---|---|
| 1 | VCC | Input | 3.3 V supply |
| 2 | GND | — | Ground |
| 3 | SCK | Input | SPI Clock |
| 4 | MISO | Output | SPI Master In / Slave Out |
| 5 | MOSI | Input | SPI Master Out / Slave In |
| 6 | NSS | Input | SPI Chip Select (active low) |
| 7 | DIO0 | Output | TX Done / RX Done interrupt |
| 8 | DIO1 | Output | RX Timeout / FIFO Level interrupt (not used) |
| 9 | DIO2 | Output | FIFO Full interrupt (not used) |
| 10 | DIO3 | Output | CAD Done interrupt (not used) |
| 11 | DIO4 | Output | PLL Lock interrupt (not used) |
| 12 | DIO5 | Output | Mode Ready interrupt (not used) |
| 13 | RST | Input | Hardware reset (active low) |
| 14 | ANT | I/O | RF antenna connection (50 Ω) |

---

## 433MHz Rubber Duck SMA Antenna

| Specification | Value |
|---|---|
| Type | Rubber Duck / Flexible whip |
| Frequency | 433 MHz |
| Impedance | 50 Ω |
| Gain | ~3 dBi (improved over spring) |
| Connector | SMA (male) — mates with U.FL/SMA adapter |
| Radiation Pattern | Omnidirectional |

### Antenna Handling Notes
- Always connect the antenna before powering the RA-02 when transmitting
- Connect via the U.FL/IPEX to SMA Adapter Cable (see below)
- Position the antenna vertically for maximum omnidirectional coverage
- Keep the antenna ≥ 5 cm away from the ESP32 board, USB cable, and power bank to minimize RF interference

---

## U.FL/IPEX to SMA Adapter Cable

| Specification | Value |
|---|---|
| Purpose | Connect RA-02 U.FL antenna connector to external SMA antenna |
| Connector A | U.FL / IPEX (female, mates with RA-02 onboard IPX connector) |
| Connector B | SMA (female, accepts SMA male from Rubber Duck antenna) |
| Impedance | 50 Ω |

### Notes
- Essential for using the Rubber Duck SMA Antenna with the RA-02 module
- Press the U.FL connector firmly onto the RA-02 IPX socket until it clicks
- Secure the adapter cable with hot glue or a cable tie to prevent accidental disconnection in the field

---

## INA219 Current Sensor

The INA219 is a precision current/power monitor IC used to measure the node's power consumption in real time.

| Specification | Value |
|---|---|
| Interface | I2C |
| I2C Address (default) | 0x40 |
| Shunt Voltage Range | ±320 mV |
| Bus Voltage Range | 0–26 V |
| Current Resolution | 1 mA (default config) |
| Power Resolution | 2 mW |
| Calibration | Register-based |
| Alert Pin | No |
| Supply Voltage | 3.0–5.5 V |

### Monitored Values

| Metric | Unit |
|---|---|
| Bus Voltage | V |
| Shunt Current | mA |
| Power Consumption | mW |
| Estimated Device Health | Derived |

### Recommended Shunt Resistor

A 0.1 Ω, 1% precision resistor is placed in series between the Power Bank output and the ESP32 VIN. This provides current measurement with minimal voltage drop at typical operating currents.

```
Power Bank (+5V) → [0.1Ω Shunt] → INA VIN+ → INA VIN- → ESP32 VIN
                                       |
                                     I2C → ESP32
```

---

## Power Bank

| Specification | Requirement |
|---|---|
| Output Voltage | 5 V USB |
| Output Current | ≥ 1 A (2 A recommended) |
| Capacity | ≥ 5000 mAh recommended |
| Connector | USB-A or USB-C to Micro-USB / USB-C cable |
| Pass-through Charging | Not required |

### Runtime Estimation

At 160 mA average draw (ESP32 active + LoRa RX):

| Power Bank Capacity | Estimated Runtime |
|---|---|
| 5000 mAh | ~31 hours |
| 10000 mAh | ~62 hours |
| 20000 mAh | ~125 hours |

*Estimates assume 80% power bank efficiency and 5V to 3.3V conversion losses.*

---

## Approximate Cost Per Node (BDT)

| Component | Approximate Cost |
|---|---|
| ESP32 V1.3 Dev Board (CH340C) | ~580 BDT |
| SX1278 RA-02 433MHz LoRa Module | ~859 BDT |
| 433MHz Rubber Duck SMA Antenna | ~160 BDT |
| U.FL/IPEX to SMA Adapter Cable | ~150–160 BDT |
| INA219 Current Sensor | ~150–250 BDT |
| **Total per node** | **~2000 BDT** |

> Power Bank is excluded — assumed to be already available.
