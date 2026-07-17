# LoRa Communication

**Module:** SX1278 RA-02  
**Frequency:** 433 MHz  
**Modulation:** LoRa (Chirp Spread Spectrum)  
**Library:** arduino-LoRa (Sandeep Mistry)  

---

## SX1278 Module Overview

The SX1278 is a long-range, low-power transceiver manufactured by Semtech, using their proprietary LoRa (Long Range) modulation based on Chirp Spread Spectrum (CSS).

| Specification | Value |
|---|---|
| Chipset | Semtech SX1278 |
| Module | RA-02 (AI Thinker) |
| Frequency Band | 433 MHz (ITU Region 1) |
| Modulation | LoRa CSS |
| Output Power | +20 dBm (100 mW) |
| Receiver Sensitivity | –148 dBm (maximum) |
| Supply Voltage | 3.3 V |
| Interface | SPI |
| Operating Current (TX) | 120 mA (max, +20 dBm) |
| Operating Current (RX) | 10.8 mA |
| Sleep Current | 0.2 µA |

---

## Why 433 MHz

| Factor | 433 MHz | 915 MHz | 2.4 GHz |
|---|---|---|---|
| Propagation distance | Longest | Medium | Shortest |
| Obstacle penetration | Best | Good | Limited |
| Antenna size | Larger (~17 cm) | Medium (~8 cm) | Smallest (~3 cm) |
| Regulatory (South Asia, Middle East, Africa) | Generally permitted | Restricted or unavailable | Permitted |
| Interference | Low (less crowded band) | Moderate | High (Wi-Fi, BT) |

433 MHz is selected for maximum range and penetration in field conditions, particularly through vegetation, rubble, and buildings common in disaster scenarios.

---

## RF Parameters

| Parameter | Configured Value | Notes |
|---|---|---|
| Frequency | 433.0 MHz | Center frequency |
| Spreading Factor (SF) | 10 | Balance between range and data rate |
| Bandwidth (BW) | 125 kHz | Standard bandwidth |
| Coding Rate (CR) | 4/5 | Lowest overhead CR |
| Sync Word | 0xF3 | Custom — prevents cross-network interference |
| CRC | Enabled | Hardware CRC on payload |
| TX Power | +17 dBm | Practical maximum without violating limits |
| Preamble Length | 8 symbols | Standard |

### Data Rate Calculation (SF10, BW125, CR4/5)

```
Bit Rate = SF × BW / (2^SF) × CR
         = 10 × 125,000 / 1024 × (4/5)
         ≈ 976 bps
```

This data rate is intentionally low to maximize range and link budget.

---

## Estimated Range

| Environment | Expected Range |
|---|---|
| Open field, line of sight | 4–6 km |
| Rural area, low vegetation | 2–4 km |
| Suburban, mixed obstacles | 1–2 km |
| Dense urban, multi-story buildings | 500 m – 1 km |
| Indoor (same building) | 50–200 m |

Range is calculated assuming:
- TX power: +17 dBm
- 433 MHz spring antenna (gain ≈ 2 dBi) on both ends
- Receiver sensitivity: –137 dBm (at SF10)
- Link budget: ≈ 156 dB

---

## LoRa Operating Modes

The SX1278 operates in one of three modes at any given time:

| Mode | Description | Current |
|---|---|---|
| Standby | Radio idle, SPI accessible | ~1.8 mA |
| Receive Continuous | Listening for incoming packets | ~10.8 mA |
| Transmit | Sending a packet | ~120 mA (peak) |

After each TX operation, the firmware immediately returns the radio to Receive Continuous mode via `LoRa.receive()`.

---

## Antenna

**Type:** 433 MHz Spring (Helical) Antenna  
**Gain:** ~2 dBi  
**Impedance:** 50 Ω  
**Connector:** IPX/U.FL on RA-02 module  

> **Important:** The SX1278 RA-02 module must never be powered with the antenna disconnected while transmitting. Doing so can damage the RF output stage due to impedance mismatch and reflected power.

### Antenna Orientation
For maximum range, orient the antenna vertically (perpendicular to the ground plane). Horizontal polarization reduces effective range by 20–30%.

---

## SPI Communication

The ESP32 communicates with the SX1278 over the hardware SPI bus.

| Signal | ESP32 GPIO | SX1278 Pin | Description |
|---|---|---|---|
| SCK | GPIO18 | SCK | SPI Clock |
| MISO | GPIO19 | MISO | Master In, Slave Out |
| MOSI | GPIO23 | MOSI | Master Out, Slave In |
| NSS | GPIO5 | NSS | Chip Select (active low) |
| RST | GPIO14 | RST | Hardware Reset |
| DIO0 | GPIO26 | DIO0 | TX Done / RX Done IRQ |

SPI clock frequency: 8 MHz (default for arduino-LoRa). The SX1278 supports up to 10 MHz SPI.

---

## Packet Timing

At SF10, BW125, a typical 50-byte packet takes approximately:

```
ToA (Time on Air) ≈ 330 ms
```

This limits throughput to approximately 3 packets per second. The firmware enforces a minimum inter-packet delay of 400 ms to prevent receiver desensitization.

### Channel Activity Detection (CAD)

Before transmitting, the firmware can optionally run CAD to detect whether the channel is busy. CAD takes ~2 ms and prevents packet collisions in high-density deployments. CAD is enabled by default when more than 3 nodes are detected in the mesh.

---

## DIO0 Interrupt Handling

```cpp
void IRAM_ATTR onLoRaReceiveISR(int packetSize) {
    if (packetSize == 0) return;

    RawPacket raw;
    raw.rssi = LoRa.packetRssi();
    raw.snr  = LoRa.packetSnr();
    raw.size = packetSize;

    for (int i = 0; i < packetSize && i < MAX_PACKET_SIZE; i++) {
        raw.data[i] = LoRa.read();
    }

    BaseType_t woken;
    xQueueSendFromISR(xQueueLoRaRx, &raw, &woken);
    portYIELD_FROM_ISR(woken);
}
```

The ISR is marked `IRAM_ATTR` to ensure it executes from IRAM rather than flash, preventing cache miss delays during interrupt handling.

---

## Signal Quality Monitoring

Each received packet is tagged with:

| Metric | Description | Typical Range |
|---|---|---|
| RSSI | Received Signal Strength Indicator | –60 to –120 dBm |
| SNR | Signal-to-Noise Ratio | +10 to –20 dB |

These values are forwarded to the Android app and displayed in the node status panel. RSSI and SNR are used by the mesh router to prefer higher-quality paths when multiple routes are available.

---

## Regulatory Notes

The 433 MHz ISM band is used in most countries (ITU Region 1: Europe, Africa, Middle East, Russia; and parts of Region 3: Asia). Maximum permitted output power varies:

| Region | Max TX Power (EIRP) |
|---|---|
| European Union (ETSI EN 300 220) | +10 dBm (10 mW), duty cycle limited |
| Bangladesh / South Asia (general) | Unlicensed ISM use, check local BTRC regulations |
| USA (FCC Part 15) | 433 MHz is not the primary ISM band — use 915 MHz |

> The configured TX power of +17 dBm may exceed the duty-cycle limits in some jurisdictions. Verify local regulations before deployment. For emergency use in disaster conditions, operators may be granted regulatory exemptions.
