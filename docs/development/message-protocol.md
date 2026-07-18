# Message Protocol & Packet System — Phase A12

## Protocol Layers Overview

To maintain decoupling between the high-level application modules and low-level physical transceivers (Bluetooth/LoRa), the network operates a modular protocol stack:

```
 ┌───────────────────────────┐
 │     Application Layer     │  (Instantiates MessageDomainModel)
 └─────────────┬─────────────┘
               │ mapping
 ┌─────────────▼─────────────┐
 │     Message Protocol      │  (Packages fields into Packet models)
 └─────────────┬─────────────┘
               │ serialization
 ┌─────────────▼─────────────┐
 │       Packet Layer        │  (Serializes to standard JSON strings)
 └─────────────┬─────────────┘
               │ write payload
 ┌─────────────▼─────────────┐
 │      Transport Layer      │  (Transmits raw bytes over BLE/LoRa)
 └───────────────────────────┘
```

---

## Packet Frame Layout

Standard packets enclose three primary fields:
1. **Header**: Metadata containing routing instructions.
2. **Payload**: Encrypted data bytes (prepared for future authenticated encryption).
3. **Footer**: Checksum verifying frame integrity.

### Header Schema Parameters
- `v` (Version): Protocol iteration identifier (default `1`).
- `pid` (Packet ID): Prefixed unique packet string key.
- `sid` (Sender ID): Unique user ID initiating the message.
- `rid` (Receiver ID): Unique user/node ID target.
- `mt` (Message Type): Enums matching `TEXT`, `VOICE`, `LOCATION`, `SOS`, `RESOURCE`, `STATUS`, `SYSTEM`.
- `pr` (Priority): Enums matching `CRITICAL` (SOS), `HIGH` (Emergency), `NORMAL` (Chat), `LOW` (Telemetry).
- `ttl` (Time-to-Live): Epoch expiration timestamp.
- `hc` (Hop Count): Incremented by each mesh node forwarding the packet.
- `ts` (Timestamp): Epoch creation timestamp.

---

## Serialized JSON Representation Example

Standard frame serialized for Bluetooth BLE or LoRa nodes:

```json
{
  "h": {
    "v": 1,
    "pid": "pkt_e9f2988",
    "sid": "usr_9c80d88",
    "rid": "usr_38b29e2",
    "mt": "TEXT",
    "pr": "NORMAL",
    "ttl": 1781890000,
    "hc": 0,
    "ts": 1781803600
  },
  "p": "aGVsbG8gd29ybGQgbWVzaCBwYWNrZXQ=",
  "tag": "YXV0aF90YWdfcGxhY2Vob2xkZXI=",
  "crc": 3892019902
}
```

---

## Reliability & Duplicate Prevention

To prevent broadcast loops (infinite storm forwarding) under outdoor settings:
1. **Deduplication Check**: Nodes maintain a thread-safe cache ([`DuplicateDetector`](../../android/app/src/main/java/com/mesh/emergency/core/protocol/DuplicateDetector.kt)). Discovered frames with matching packet IDs are dropped immediately.
2. **TTL Validation**: Nodes check packet deadlines ([`PacketValidator`](../../android/app/src/main/java/com/mesh/emergency/core/protocol/PacketValidator.kt)) and drop expired frames.

---

## Future Binary Protocol Migration Plan

Although JSON is highly readable for development mode diagnostics, LoRa constraints (e.g. 256-byte maximum transmission unit) require a compact binary format. The future binary protocol will pack fields sequentially:

```
┌─────────┬───────────┬───────────┬───────────┬───────────┬─────────┬─────────┬─────────────┐
│ Version │ Packet ID │ Sender ID │ Recipient │ Type/Prio │   TTL   │ Payload │ Checksum    │
│ (1 byte)│ (16 bytes)│ (16 bytes)│ (16 bytes)│ (1 byte)  │(4 bytes)│ (var)   │ (4 bytes)   │
└─────────┴───────────┴───────────┴───────────┴───────────┴─────────┴─────────┴─────────────┘
```
This binary migration will execute at the lower data adapters layer without modifying domain interfaces.
