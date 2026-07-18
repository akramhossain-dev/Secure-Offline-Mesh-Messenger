# LoRa Module

This document outlines the Semtech SX1278 RA-02 LoRa transceiver specifications, SPI connection pathways, and antenna practices.

---

## 1. Module Specifications (RA-02 AI-Thinker)

* **RF Chipset**: Semtech SX1278.
* **Modulation**: LoRa CSS (Chirp Spread Spectrum).
* **Frequency Range**: 433 MHz.
* **Maximum Output Power**: +20 dBm (configured to +17 dBm for thermal safety and power bank stability).
* **Interface**: SPI.
* **Sensitivity**: Down to -148 dBm.

---

## 2. SPI Connections Table

The ESP32 communicates as the SPI Master, and the SX1278 acts as the Slave. The wiring configuration is detailed below:

| SX1278 Pin | ESP32 GPIO | Color Code (Suggested) | Role |
|---|---|---|---|
| **VCC** | 3.3V | Red | 3.3V power (Never connect to 5V rail). |
| **GND** | GND | Black | Common ground connection. |
| **SCK** | GPIO18 | Yellow | SPI Clock. |
| **MISO** | GPIO19 | Blue | SPI Master In / Slave Out. |
| **MOSI** | GPIO23 | Green | SPI Master Out / Slave In. |
| **NSS (CS)** | GPIO5 | Orange | Chip Select (active low). |
| **RST** | GPIO14 | Purple | Hardware Reset. |
| **DIO0** | GPIO26 | White | TX Done / RX Done interrupt trigger. |

---

## 3. Coaxial Adapter and Antenna

### A. U.FL/IPEX to SMA Female Adapter Cable
* The SX1278 RA-02 has a miniature U.FL/IPEX antenna socket on-board.
* The adapter cable bridges the U.FL connector to a standard chassis-mountable SMA Female port.
* **Care note**: U.FL connectors are fragile. Press vertically until a click is felt. A drop of hot glue can be applied to secure it permanently.

### B. 433MHz Rubber Duck SMA Antenna
* A high-gain (~3 dBi), 50 Ω flexible whip antenna.
* **Warning**: Never transmit without the antenna connected. Doing so causes power reflections that can destroy the RF output amplifier.
* Orientation should be kept vertical to ensure omnidirectional propagation.
