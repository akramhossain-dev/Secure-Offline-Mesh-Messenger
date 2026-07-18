# Communication Overview

The Offline Emergency Mesh Communication System achieves reliable off-grid messaging by combining localized high-bandwidth connections with a long-range decentralized radio network.

---

## 1. Network Topology

The network operates on a tiered topology linking two physical layers:
1. **Local Access Layer (Bluetooth BLE)**: Bridges Android smartphones to localized ESP32 mesh nodes.
2. **Mesh Transport Layer (LoRa 433MHz)**: Relays packets between ESP32 nodes across several kilometers.

```
+-------------+         Bluetooth BLE         +-------------+
| Android App | <───────────────────────────> |  ESP32 Node |
+-------------+                               +------+------+
                                                     |
                                                     | SPI Connection
                                                     ▼
                                              +-------------+
                                              | SX1278 LoRa |
                                              +------+------+
                                                     |
                                                     | 433MHz RF Mesh
                                                     ▼
                                            +-----------------+
                                            | LoRa Mesh Nodes |
                                            +-----------------+
```

---

## 2. Dynamic Transport Selection

All messaging traffic is orchestrated by the Android application's `CommunicationManager`. Outbound packets are prioritized and routed automatically through the most reliable and efficient transport channel:

1. **Bluetooth Transport**: Active if the destination node's BLE MAC/advertising signature is detected nearby.
2. **LoRa Transport**: Fallback channel routing traffic through the ESP32 bridge over the 433MHz LoRa mesh network.
3. **Store & Forward**: If a path to the receiver is currently unavailable, the message is stored locally on the device and retried once the recipient returns to the network.

---

## 3. Communication Directory Index

To understand the specifics of each transport and protocol component, refer to the following documents:

* [Bluetooth Transport](bluetooth-transport.md): GATT specifications, scan flows, and BLE connections.
* [LoRa Transport](lora-transport.md): Semtech SX1278 radio parameter configurations and range links.
* [Hybrid Communication](hybrid-communication.md): In-depth review of `CommunicationManager` state selection.
* [Message Protocol](message-protocol.md): Routing rules, deduplication, and message generation sequences.
* [Packet Structure](packet-structure.md): Header reference tables and JSON envelope definitions.
* [Store & Forward System](store-forward-system.md): Background workers, SQLite storage schema, and retry schedules.
