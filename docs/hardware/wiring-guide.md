# Wiring Guide

---

## Power Flow

```
Power Bank (5V USB)
        |
    USB Cable
        |
    ESP32 USB Port (5V → 3.3V internal regulator)
        |
   3.3V Pin
        |
   SX1278 VCC
```

The ESP32 on-board AMS1117 LDO regulator converts 5V USB input to 3.3V for its own logic and for powering the SX1278. **Do not connect the SX1278 VCC directly to the USB 5V rail** — the SX1278 is a 3.3V device only.

---

## SX1278 → ESP32 Wiring Table

| SX1278 RA-02 Pin | ESP32 GPIO | Wire Color (suggested) | Notes |
|---|---|---|---|
| VCC | 3.3V | Red | 3.3V only — never 5V |
| GND | GND | Black | Common ground |
| SCK | GPIO18 | Yellow | SPI Clock |
| MISO | GPIO19 | Blue | SPI MISO |
| MOSI | GPIO23 | Green | SPI MOSI |
| NSS | GPIO5 | Orange | Chip Select (active low) |
| RST | GPIO14 | Purple | Hardware Reset |
| DIO0 | GPIO26 | White | TX/RX Done Interrupt |

---

## INA219 → ESP32 Wiring Table

| INA Pin | ESP32 GPIO | Notes |
|---|---|---|
| VCC | 3.3V | Sensor supply |
| GND | GND | Common ground |
| SDA | GPIO21 | I2C Data |
| SCL | GPIO22 | I2C Clock |
| VIN+ | Power Bank (+) | Positive power input |
| VIN- | ESP32 VIN | After shunt resistor |

> Place a 0.1 Ω shunt resistor between VIN+ and VIN−. The INA219 measures the voltage drop across this resistor to calculate current.

---

## Full Wiring Diagram (Text Representation)

```
                    ┌──────────────────────────┐
                    │      ESP32 V1.3          │
                    │                          │
Power Bank ─USB─►  │ VIN(5V)    3.3V ─────────┼──────► SX1278 VCC
                    │ GND ───────────────────── ┼──────► SX1278 GND
                    │                          │
                    │ GPIO18 (SCK) ────────────┼──────► SX1278 SCK
                    │ GPIO19 (MISO) ───────────┼──────► SX1278 MISO
                    │ GPIO23 (MOSI) ───────────┼──────► SX1278 MOSI
                    │ GPIO5  (NSS) ────────────┼──────► SX1278 NSS
                    │ GPIO14 (RST) ────────────┼──────► SX1278 RST
                    │ GPIO26 (DIO0) ───────────┼──────► SX1278 DIO0
                    │                          │
                    │ GPIO21 (SDA) ────────────┼──────► INA SDA
                    │ GPIO22 (SCL) ────────────┼──────► INA SCL
                    │ 3.3V ────────────────────┼──────► INA VCC
                    │ GND ─────────────────────┼──────► INA GND
                    └──────────────────────────┘

                        SX1278 RA-02
                    ┌────────────────┐
                    │ VCC   GND      │
                    │ SCK   MISO     │
                    │ MOSI  NSS      │
                    │ RST   DIO0     │
                    │       ANT ─────┼──► U.FL/IPEX Cable ──► SMA Adapter ──► 433MHz Rubber Duck SMA Antenna
                    └────────────────┘
```

---

## SPI Bus Notes

- The SX1278 is the **only SPI device** on the bus in this design. No other SPI peripherals share GPIO18/19/23.
- GPIO5 (NSS) must be pulled HIGH at boot on the ESP32 to avoid boot mode conflicts. The arduino-LoRa library handles NSS correctly.
- GPIO15 on the ESP32 is also used for boot strapping — avoid wiring it to SX1278 DIO pins.
- SPI operates at 8 MHz by default (arduino-LoRa). The SX1278 supports up to 10 MHz.

---

## I2C Bus Notes

- GPIO21 (SDA) and GPIO22 (SCL) are the default I2C pins for ESP32.
- The INA219 I2C address is `0x40` by default (A0 and A1 pins floating/GND).
- Pull-up resistors (4.7 kΩ to 3.3V) are required on SDA and SCL lines. Some INA219 breakout boards include them — check your module.
- If pull-ups are not on the breakout board, add 4.7 kΩ resistors from SDA → 3.3V and SCL → 3.3V on the breadboard.

---

## GPIO Boot Constraints

The following ESP32 GPIOs have boot-time constraints. Do not connect them to devices that drive them during power-up:

| GPIO | Boot Constraint |
|---|---|
| GPIO0 | Must be HIGH at boot (floating or pull-up). Do not connect to SX1278 |
| GPIO2 | Must be LOW during flash download mode. Avoid |
| GPIO15 | Must be HIGH at boot. Avoid for SX1278 DIO pins |
| GPIO5 (NSS) | Must be HIGH at boot — SX1278 NSS is active low, so this is compatible when the chip is idle |

All chosen GPIO assignments (18, 19, 23, 5, 14, 26, 21, 22) are safe for this project.

---

## Breadboard Layout

For prototype assembly on a 830-point breadboard:

1. Place ESP32 Dev Board spanning the center divide
2. Place SX1278 RA-02 on the right side of the breadboard
3. Place INA219 breakout board below the SX1278
4. Run power rails: 3.3V from ESP32 to the left power rail; GND to the right power rail
5. Use the power rails to supply VCC and GND to both the SX1278 and INA

### Suggested Breadboard Column Assignment

| Column Range | Component |
|---|---|
| A1–A30 | Left ESP32 pins |
| J1–J30 | Right ESP32 pins |
| A32–A40 | SX1278 RA-02 |
| A42–A48 | INA219 |
| + Rail (left) | 3.3V from ESP32 |
| – Rail (left) | GND |

---

## Antenna Placement

- Attach the U.FL/IPEX to SMA Adapter Cable to the IPX connector on the SX1278 RA-02 **before powering on**
- Connect the 433MHz Rubber Duck SMA Antenna to the SMA end of the adapter cable
- Position the antenna **vertically** for omnidirectional radiation
- Keep the antenna ≥ 5 cm away from the ESP32 board, USB cable, and power bank to minimize RF interference
- Secure the U.FL connector with a small dab of hot glue to prevent accidental disconnection

---

## Power Monitoring Inline Connection

The INA219 is placed **inline** between the Power Bank and the ESP32 VIN:

```
Power Bank (+5V USB)
    |
    └── [VIN+] INA219 [VIN−] ── [0.1Ω Shunt] ── ESP32 VIN/USB
```

In practice for breadboard prototyping:
1. Cut a USB cable, exposing the +5V and GND wires
2. Route the +5V wire through the INA's VIN+ → Shunt → VIN− path
3. GND connects directly from Power Bank to ESP32 GND

Alternatively, if using the ESP32 USB port directly (not bare VIN), power monitoring can measure current from a USB power meter as an external tool during testing, and the INA219 can monitor the 3.3V rail instead with an inline shunt resistor on the 3.3V output.
