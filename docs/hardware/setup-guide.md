# Hardware Setup Guide

---

## Prerequisites

Before assembling the hardware, ensure you have:

- [ ] ESP32 V1.3 Dev Board (CH340C)
- [ ] SX1278 RA-02 433 MHz LoRa module
- [ ] 433 MHz spring antenna (with U.FL connector)
- [ ] INA219 or INA226 breakout board
- [ ] 830-point breadboard
- [ ] Jumper wires (male-to-male and male-to-female)
- [ ] USB cable (Micro-USB to USB-A, or USB-C depending on ESP32 board)
- [ ] Power bank (5V USB, ≥ 2A output)
- [ ] 0.1 Ω shunt resistor (for INA219/226 inline measurement)
- [ ] 4.7 kΩ resistors × 2 (I2C pull-ups, if not on INA breakout)
- [ ] PC with PlatformIO or Arduino IDE installed

---

## Step 1: Inspect Components

### ESP32 Board
- Verify the CH340C USB-Serial bridge chip is visible on the board
- Check that all 38 GPIO pins are present and not bent
- Confirm the USB port (micro-USB or USB-C) is intact

### SX1278 RA-02
- Confirm the U.FL antenna connector is not cracked
- Check that all solder pads/pins are clean and accessible
- The RA-02 module has SMD components — handle with care and avoid static discharge

### INA219/226 Breakout
- Check for visible shunt resistor on the board (some breakouts include a 0.1 Ω shunt)
- Confirm I2C pull-up resistors are present (look for two resistors near SDA/SCL)
- Identify VIN+, VIN−, VCC, GND, SDA, SCL pins

---

## Step 2: Attach the Antenna

> **Always attach the antenna before powering on the SX1278.**

1. Take the 433 MHz spring antenna with U.FL connector
2. Align the U.FL connector with the IPX socket on the RA-02 module
3. Press down firmly until you hear/feel a click
4. The antenna should be secure and not rotate freely

---

## Step 3: Breadboard Assembly

### Place Components

1. Insert the ESP32 Dev Board lengthwise across the center divide of the breadboard
   - One column of pins on each side of the center gap
   - Pins should grip firmly in the breadboard sockets
2. Insert the SX1278 RA-02 module on the right side of the breadboard, below the ESP32
   - Use male headers soldered to the RA-02 pads if the module does not have through-hole pins
3. Insert the INA219/226 breakout board below the SX1278

### Connect Power Rails

Using jumper wires:
1. Connect the ESP32 **3.3V** pin → left breadboard power rail (+)
2. Connect the ESP32 **GND** pin → left breadboard power rail (−)
3. Connect the SX1278 **VCC** → left breadboard power rail (+)
4. Connect the SX1278 **GND** → left breadboard power rail (−)
5. Connect the INA219 **VCC** → left breadboard power rail (+)
6. Connect the INA219 **GND** → left breadboard power rail (−)

---

## Step 4: SPI Wiring (SX1278 ↔ ESP32)

Using individual jumper wires, make the following connections:

| From | To | Color |
|---|---|---|
| SX1278 SCK | ESP32 GPIO18 | Yellow |
| SX1278 MISO | ESP32 GPIO19 | Blue |
| SX1278 MOSI | ESP32 GPIO23 | Green |
| SX1278 NSS | ESP32 GPIO5 | Orange |
| SX1278 RST | ESP32 GPIO14 | Purple |
| SX1278 DIO0 | ESP32 GPIO26 | White |

Check each connection by tracing the wire from the SX1278 pad to the correct ESP32 GPIO. Incorrect SPI wiring is the most common cause of LoRa initialization failure.

---

## Step 5: I2C Wiring (INA219/226 ↔ ESP32)

| From | To |
|---|---|
| INA SDA | ESP32 GPIO21 |
| INA SCL | ESP32 GPIO22 |

If I2C pull-up resistors are **not** on the breakout board:
- Connect a 4.7 kΩ resistor from GPIO21 (SDA) to the 3.3V power rail
- Connect a 4.7 kΩ resistor from GPIO22 (SCL) to the 3.3V power rail

---

## Step 6: Power Monitoring Inline Connection

To measure current drawn by the ESP32 + SX1278 system:

1. Route the Power Bank +5V USB line through the INA's measurement path:
   ```
   Power Bank (+5V) → INA VIN+ → [0.1Ω shunt on breakout] → INA VIN− → ESP32 USB VIN
   ```
2. The GND line runs directly from Power Bank to ESP32 GND (not through INA)

**Simpler Alternative for Initial Testing:**
Skip the inline INA connection initially. Power the ESP32 via USB directly from the Power Bank, and power the INA from the 3.3V rail with VIN+ connected to the 3.3V rail and VIN− after a shunt to GND. This measures internal 3.3V current only.

---

## Step 7: Visual Inspection Checklist

Before first power-on:

- [ ] Antenna connected to SX1278
- [ ] SX1278 VCC → 3.3V only (not 5V)
- [ ] All SPI pins connected as per the wiring table
- [ ] DIO0 connected to GPIO26
- [ ] RST connected to GPIO14
- [ ] INA219 SDA/SCL connected with pull-ups present
- [ ] No shorts between adjacent GPIO pins
- [ ] No loose or disconnected wires

---

## Step 8: First Power-On

1. Connect the USB cable from the Power Bank to the ESP32
2. The ESP32 blue LED (GPIO2) should blink briefly on boot
3. Open Arduino IDE or PlatformIO Serial Monitor at **115200 baud**
4. Flash the firmware (see firmware setup documentation)
5. Observe serial output:
   ```
   [BOOT] Device Identity loaded. Node ID: a3f2c1d0-...
   [LORA] SX1278 initialized. Frequency: 433MHz
   [BLE]  GATT Server started. Advertising...
   [INA]  Power monitor started. Voltage: 3.31V
   ```

### Common Boot Errors

| Error Message | Cause | Fix |
|---|---|---|
| `LoRa init failed` | NSS/RST/DIO0 wiring wrong | Re-check GPIO5, GPIO14, GPIO26 |
| `SPI timeout` | MOSI or SCK disconnected | Re-check GPIO18, GPIO19, GPIO23 |
| `INA not found at 0x40` | SDA/SCL reversed or missing pull-ups | Swap SDA/SCL or add 4.7k pull-ups |
| `BLE init failed` | Insufficient RAM | Verify ESP32 is running correct firmware |

---

## Step 9: Firmware Flash

### PlatformIO

```bash
# Install dependencies
pio lib install "LoRa" "ArduinoJson" "INA219" "NimBLE-Arduino"

# Build and upload
pio run --target upload --upload-port /dev/ttyUSB0

# Open serial monitor
pio device monitor --baud 115200
```

### Arduino IDE

1. Install ESP32 board support via Boards Manager (URL: `https://dl.espressif.com/dl/package_esp32_index.json`)
2. Install libraries: `LoRa`, `ArduinoJson`, `Adafruit INA219`, `NimBLE-Arduino`
3. Select Board: **ESP32 Dev Module**
4. Select Upload Speed: **921600**
5. Select Port: the COM/ttyUSB port for the ESP32
6. Upload

---

## Step 10: Prototype Validation

After firmware is running, verify each subsystem:

### LoRa Validation
- Serial output should show `[LORA] Listening...`
- Flash a second ESP32 node with the same firmware
- Send a TEST packet from one node — the other should log `[MESH] Received packet from <NodeID>`

### BLE Validation
- Open Android app and scan for BLE devices
- The ESP32 should appear with the advertised name `MeshNode-<shortID>`
- Tap to connect — serial monitor shows `[BLE] Client connected`

### INA Validation
- Serial monitor shows periodic `[POWER] V=3.31V I=152mA P=503mW`
- Android app Power Monitor screen displays these values

---

## Prototype vs Permanent Build

| Aspect | Breadboard Prototype | Soldered Build |
|---|---|---|
| Assembly time | 30 minutes | 2–4 hours |
| Reliability | Lower (wire pullout) | High |
| Field use | Indoor testing only | Outdoor deployment |
| Modification | Easy | Requires desoldering |
| Recommended for | Development, testing | Field deployment |

### Soldering Guide (for Permanent Build)

1. Solder male headers to SX1278 RA-02 pads if not pre-soldered
2. Use a custom PCB or perfboard to mount ESP32 and SX1278 side-by-side
3. Use 26 AWG stranded wire for SPI connections (flexible, breakage-resistant)
4. Apply heat shrink tubing to all exposed solder joints
5. Secure the antenna U.FL connector with a small dab of hot glue to prevent accidental disconnection
6. Use a 3D-printed or weatherproof enclosure for outdoor deployment
