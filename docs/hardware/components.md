# Hardware Components

---

## Component List

| # | Component | Specification | Purpose |
|---|---|---|---|
| 1 | ESP32 Dev Board | V1.3, CH340C, NodeMCU-32S | Main controller, BLE host, SPI master |
| 2 | SX1278 LoRa Module | RA-02, 433 MHz, AI Thinker | Long-range RF transceiver |
| 3 | Spring Antenna | 433 MHz, ~17 cm, 50 Ω | RF signal transmission and reception |
| 4 | INA219 / INA226 | Current sensor, I2C | Power monitoring |
| 5 | Power Bank | 5V USB output, ≥ 5000 mAh | Power supply |
| 6 | USB Cable | Micro-USB or USB-C | Power delivery to ESP32 |
| 7 | Jumper Wires | Male-to-Male, Male-to-Female | Breadboard connections |
| 8 | Breadboard | 830-point | Prototype assembly |

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

## 433 MHz Spring Antenna

| Specification | Value |
|---|---|
| Type | Spring / Helical whip |
| Frequency | 433 MHz |
| Impedance | 50 Ω |
| Gain | ~2 dBi |
| Length | ~17 cm (quarter-wave: 300/433 × 0.25 ≈ 17.3 cm) |
| Connector | IPX / U.FL (mates with RA-02) |
| Radiation Pattern | Omnidirectional |

### Antenna Handling Notes
- Always connect the antenna before powering the RA-02 when transmitting
- Do not bend the spring antenna at sharp angles — this degrades resonance
- Position the antenna away from the ESP32 board and USB cable to minimize interference
- For fixed node deployments, a vertical whip antenna improves range versus a coiled spring

---

## INA219 / INA226 Current Sensor

Both INA219 and INA226 are compatible with this project. The INA226 offers higher precision and configurable alert thresholds.

| Specification | INA219 | INA226 |
|---|---|---|
| Interface | I2C | I2C |
| I2C Address (default) | 0x40 | 0x40 |
| Shunt Voltage | ±320 mV | ±81.92 mV |
| Bus Voltage | 0–26 V | 0–36 V |
| Current Resolution | 1 mA (default config) | 1.25 mA (default) |
| Power Resolution | 2 mW | Calculated |
| Calibration | Register-based | Register-based |
| Alert Pin | No | Yes |
| Supply Voltage | 3.0–5.5 V | 2.7–5.5 V |

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
