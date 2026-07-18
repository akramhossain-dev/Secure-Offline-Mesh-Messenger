# Hardware Integration Preparedness Guide
**Offline Emergency Mesh Communication System — Phase A35**

This document establishes the interface specs, handshake flows, and packet structures prepared for upcoming hardware integrations (**Series B: ESP32 Firmware** and **Series H/I: Hardware Integration**).

---

## 1. ESP32 Integration Specifications

The Android application interfaces with the ESP32 coprocessor via a local Bluetooth Low Energy (BLE) gateway. The ESP32 is equipped with an SX1278 LoRa transceiver to route packet data over the long-range mesh network.

```
 ┌─────────────────┐           BLE            ┌─────────────┐          LoRa           ┌─────────────┐
 │                 │  ◄────────────────────►  │             │  ◄───────────────────►  │             │
 │   Android App   │   (Data Characteristics) │    ESP32    │    (SX1278 Transceiver)│  Other Node │
 │                 │                          │             │                         │             │
 └─────────────────┘                          └─────────────┘                         └─────────────┘
```

---

## 2. BLE Bridge Protocol

### GATT Service UUIDs
- **Mesh Service UUID:** `4fafc201-1fb5-459e-8fcc-c5c9c331914b`
- **TX Characteristic UUID (App -> ESP32):** `beb5483e-36e1-4688-b7f5-ea07361b26a8`
- **RX Characteristic UUID (ESP32 -> App):** `d6ee5c8e-5b12-4f99-8263-dcf7b8fa3b21`

### Handshake Flow
1. **Discovery:** Android scan detects Bluetooth advertisement showing name `MESH-NODE-XXXX`.
2. **MTU Negotiation:** Android initiates connection and requests an MTU size of **512 bytes** to prevent packet fragmentation.
3. **Descriptor Sync:** Android writes to client configuration descriptor (CCCD) on the RX Characteristic to subscribe to notifications.
4. **Identify Packet:** ESP32 sends a device capability header specifying firmware version, battery, and relay status.

---

## 3. LoRa Communication Framing

Packet sizes are constrained to a maximum of **256 bytes** to match the payload constraints of the SX1278 transceiver.

### Binary Packet Structure
All packets sent across the BLE TX characteristic follow this structure:

| Offset (Bytes) | Field Name | Type | Description |
|----------------|------------|------|-------------|
| `0` | Packet Prefix | `Byte` | Magic byte `0x7E` identifying mesh packets |
| `1` | Packet Type | `Byte` | `0x01`: Text, `0x02`: Voice, `0x03`: Location, `0x04`: Emergency |
| `2` | Hop Count | `Byte` | Initialized to `0x00`, incremented by each relay. Discarded if `> 10` |
| `3 - 18` | Destination ID | `UUID (16B)`| Target node's hardware fingerprinted ID |
| `19 - 34` | Sender ID | `UUID (16B)`| Original sender node's hardware fingerprinted ID |
| `35 - 38` | Sequence Number| `Int (4B)` | Incremental counter to reject duplicate relays |
| `39 - 251` | Ciphertext | `Bytes` | Encrypted payload (AES-256-GCM encrypted bytes) |
| `252 - 255` | Checksum | `Int (4B)` | CRC32 of the entire frame for hardware checksumming |

### Handled Payloads (Simulated character arrays)
For human-readable diagnostics during integration:
- **Location Payload:** `[LOC]|senderId|latitude|longitude|accuracy|timestamp|altitude|label`
- **Emergency Alert:** `[EMG]|senderId|type|urgencyLevel|timestamp|description`
- **Text Message:** `[MSG]|senderId|conversationId|messageId|timestamp|text`
