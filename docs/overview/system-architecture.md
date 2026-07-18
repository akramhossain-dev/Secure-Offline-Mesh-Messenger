# System Architecture

---

## Architecture Overview

The system consists of three physical layers that work together to form a resilient offline mesh network:

1. **Android Application Layer** — User-facing interface running on Android devices
2. **ESP32 Node Layer** — Embedded hardware bridge between BLE and LoRa
3. **LoRa RF Layer** — Long-range radio mesh network

```mermaid
graph TD
    subgraph UserA["User A — Android Device"]
        APP_A["Android App\n(Kotlin + Jetpack Compose)"]
    end

    subgraph NodeA["ESP32 Node A"]
        BLE_A["BLE GATT Server"]
        MCU_A["ESP32 Core\n(FreeRTOS Tasks)"]
        LORA_A["SX1278 LoRa\n(SPI)"]
        INA_A["INA219\n(I2C)"]
    end

    subgraph Mesh["LoRa 433MHz Mesh Network"]
        RELAY1["ESP32 Relay Node"]
        RELAY2["ESP32 Relay Node"]
    end

    subgraph NodeB["ESP32 Node B"]
        BLE_B["BLE GATT Server"]
        MCU_B["ESP32 Core\n(FreeRTOS Tasks)"]
        LORA_B["SX1278 LoRa\n(SPI)"]
        INA_B["INA219\n(I2C)"]
    end

    subgraph UserB["User B — Android Device"]
        APP_B["Android App\n(Kotlin + Jetpack Compose)"]
    end

    APP_A <-->|"Bluetooth BLE"| BLE_A
    BLE_A --> MCU_A
    MCU_A <-->|"SPI"| LORA_A
    INA_A -->|"I2C"| MCU_A
    LORA_A <-->|"LoRa RF 433MHz"| RELAY1
    RELAY1 <-->|"LoRa RF 433MHz"| RELAY2
    RELAY2 <-->|"LoRa RF 433MHz"| LORA_B
    LORA_B <--> MCU_B
    INA_B --> MCU_B
    MCU_B --> BLE_B
    BLE_B <-->|"Bluetooth BLE"| APP_B
```

---

## Component Interaction

### Android App ↔ ESP32 (Bluetooth BLE)

The Android app connects to the ESP32 as a BLE Central device. The ESP32 operates as a GATT Server, exposing custom service UUIDs for message transmit, message receive, and device status characteristics.

- **Transmit Characteristic**: App writes an outbound packet to this characteristic. The ESP32 reads it and queues it for LoRa transmission.
- **Notify Characteristic**: The ESP32 uses BLE notifications to push incoming LoRa packets to the connected Android app.
- **Status Characteristic**: The ESP32 exposes voltage, current, and firmware version as readable values.

The BLE connection is maintained persistently while the app is in foreground. The app handles reconnection automatically on BLE drop.

### ESP32 ↔ SX1278 (SPI)

The ESP32 communicates with the SX1278 module over the SPI bus using the hardware SPI peripheral. The LoRa library configures the SX1278 into LoRa mode and manages packet transmission and reception via interrupt-driven DIO0.

| SPI Role | Function |
|---|---|
| Master | ESP32 |
| Slave | SX1278 |
| Clock (SCK) | GPIO18 |
| MISO | GPIO19 |
| MOSI | GPIO23 |
| Chip Select (NSS) | GPIO5 |
| Reset (RST) | GPIO14 |
| IRQ (DIO0) | GPIO26 |

### ESP32 ↔ INA219 (I2C)

The INA219 current sensor sits on the I2C bus (SDA/SCL). The ESP32 polls it periodically and includes power telemetry in BLE status notifications to the Android app.

The power monitoring architecture follows this path:

```
Power Bank → INA219 → ESP32 → Bluetooth → Android App
```

| Measured Value | Source |
|---|---|
| Voltage | INA219 bus voltage register |
| Current | INA219 shunt measurement |
| Power | Calculated: V × I |
| Device Health | Derived from power readings |

---

## Communication Sequence

```mermaid
sequenceDiagram
    participant User_A as Android App (A)
    participant ESP32_A as ESP32 Node A
    participant Mesh as LoRa Mesh
    participant ESP32_B as ESP32 Node B
    participant User_B as Android App (B)

    User_A->>ESP32_A: BLE Write — Outbound Packet
    ESP32_A->>ESP32_A: Validate & Encrypt Packet
    ESP32_A->>ESP32_A: Add to TX Queue
    ESP32_A->>Mesh: LoRa Transmit — Packet
    Mesh->>Mesh: Multi-hop relay (TTL-based)
    Mesh->>ESP32_B: LoRa Receive — Packet
    ESP32_B->>ESP32_B: Decrypt & Verify Integrity
    ESP32_B->>ESP32_B: Deduplicate (seen packet ID cache)
    ESP32_B->>User_B: BLE Notify — Inbound Packet
    User_B->>ESP32_B: BLE Write — ACK Packet
    ESP32_B->>Mesh: LoRa Transmit — ACK
    Mesh->>ESP32_A: LoRa Receive — ACK
    ESP32_A->>User_A: BLE Notify — Delivery Confirmed
```

---

## Data Flow

```mermaid
flowchart LR
    subgraph Android["Android App"]
        UI["Compose UI"]
        VM["ViewModel"]
        REPO["Repository"]
        DB["Room Database"]
        BLE_MGR["BLE Manager"]
    end

    subgraph ESP32["ESP32 Firmware"]
        BLE_SRV["BLE GATT Server"]
        QUEUE["TX / RX Queues"]
        ROUTER["Mesh Router"]
        STORE["Store & Forward Cache"]
        CRYPTO["Crypto Engine"]
        SPI_DRV["SPI Driver"]
    end

    subgraph Radio["RF Layer"]
        SX1278["SX1278 LoRa"]
        ANT["433MHz Antenna"]
    end

    UI --> VM --> REPO --> DB
    REPO --> BLE_MGR
    BLE_MGR <-->|"GATT Read/Write/Notify"| BLE_SRV
    BLE_SRV --> QUEUE
    QUEUE --> ROUTER
    ROUTER --> STORE
    ROUTER --> CRYPTO
    CRYPTO --> SPI_DRV
    SPI_DRV --> SX1278
    SX1278 --> ANT
    ANT -->|"RF 433MHz"| ANT
```

---

## LoRa Mesh Topology

```mermaid
graph TB
    subgraph Zone_A["Zone A — Disaster Epicenter"]
        N1["Node 1\n(User: Ali)"]
        N2["Node 2\n(User: Sara)"]
    end

    subgraph Zone_B["Zone B — Mid Range"]
        N3["Node 3\n(Relay Only)"]
        N4["Node 4\n(User: Rescue Team)"]
    end

    subgraph Zone_C["Zone C — Command Post"]
        N5["Node 5\n(User: Command)"]
        N6["Node 6\n(User: Coordinator)"]
    end

    N1 <-->|"LoRa"| N2
    N1 <-->|"LoRa"| N3
    N2 <-->|"LoRa"| N3
    N3 <-->|"LoRa"| N4
    N3 <-->|"LoRa"| N5
    N4 <-->|"LoRa"| N5
    N5 <-->|"LoRa"| N6
```

Each node automatically participates in routing. A packet from Node 1 to Node 6 travels: `N1 → N3 → N5 → N6`, with TTL decrementing at each hop to prevent infinite loops.

---

## Network Topology Properties

| Property | Value |
|---|---|
| Topology type | Flood mesh with TTL |
| Routing algorithm | Store-and-Forward with seen-ID deduplication |
| Maximum hops (TTL) | 5 |
| Frequency | 433 MHz |
| Modulation | LoRa (CSS) |
| Network size | Theoretically unlimited (TTL-bounded per message) |
| Node roles | All nodes relay; no master node required |
| Failure mode | Graceful degradation — surviving nodes continue routing |

---

## Power Monitoring Architecture

```mermaid
flowchart TD
    PB["Power Bank\n(USB 5V)"] -->|"USB Cable"| INA["INA219\n(I2C 0x40)"]
    INA -->|"Inline"| ESP32_USB["ESP32 USB Input"]
    INA -->|"I2C SDA/SCL"| ESP32_CORE["ESP32 Core"]
    ESP32_CORE -->|"3.3V regulated"| SX1278["SX1278 LoRa Module"]
    ESP32_CORE -->|"BLE Notification"| APP["Android App\nPower Monitor Screen"]

    INA -->|"Measures"| M1["Bus Voltage (V)"]
    INA -->|"Measures"| M2["Shunt Current (mA)"]
    INA -->|"Measures"| M3["Power (mW)"]
    ESP32_CORE -->|"Derives"| M4["Device Health Status"]
```

---

## Scalability Design

The system is designed to scale from small local deployments to large regional mesh networks.

### Network Size Tiers

| Tier | Node Count | Architecture |
|---|---|---|
| Small Network | 10–100 nodes | Flat flood mesh with TTL |
| Large Network | 100–1000+ nodes | Cluster-based mesh architecture |

### Small Network (10–100 Nodes)

All nodes participate equally in routing. The flood-with-TTL approach is efficient at this scale:

- Every node forwards all non-duplicate packets
- Seen-packet cache prevents loops (128-entry circular buffer)
- TTL (default: 5 hops) bounds propagation
- Routing table not required — broadcast handles delivery

```mermaid
graph TB
    subgraph Small["Small Network (10–100 Nodes)"]
        N1["Node 1"] <--> N2["Node 2"]
        N2 <--> N3["Node 3"]
        N3 <--> N4["Node 4"]
        N1 <--> N3
        N2 <--> N4
    end
```

### Large Network (100–1000+ Nodes)

At large scale, flood routing becomes inefficient. The system uses a **cluster-based mesh architecture**:

- Nodes self-organize into geographic or logical clusters
- Each cluster has a designated **Cluster Head** node that maintains a routing table
- Inter-cluster traffic is routed via Cluster Head nodes only
- Intra-cluster traffic uses local flood routing

```mermaid
graph TB
    subgraph ClusterA["Cluster A"]
        CH_A["Cluster Head A"]
        N1A["Node"] & N2A["Node"] & N3A["Node"]
        CH_A <--> N1A
        CH_A <--> N2A
        CH_A <--> N3A
    end
    subgraph ClusterB["Cluster B"]
        CH_B["Cluster Head B"]
        N1B["Node"] & N2B["Node"] & N3B["Node"]
        CH_B <--> N1B
        CH_B <--> N2B
        CH_B <--> N3B
    end
    subgraph ClusterC["Cluster C"]
        CH_C["Cluster Head C"]
        N1C["Node"] & N2C["Node"]
        CH_C <--> N1C
        CH_C <--> N2C
    end
    CH_A <-->|"LoRa"| CH_B
    CH_B <-->|"LoRa"| CH_C
    CH_A <-->|"LoRa"| CH_C
```

### Mesh Protocol Primitives

| Primitive | Purpose |
|---|---|
| Packet ID (UUID v4) | Unique identifier for every packet |
| TTL | Limits hop count; decremented at each relay node |
| Hop Count | Tracked alongside TTL for routing metrics |
| Routing Table | Maintained by Cluster Head nodes for inter-cluster routing |
| Duplicate Detection | Seen-packet cache (sender + packet ID hash) |
| Priority Queue | TX queue ordered by priority: Critical > High > Normal > Low |
| Store and Forward | Cache undelivered packets in SPIFFS; retry on node reappearance |
