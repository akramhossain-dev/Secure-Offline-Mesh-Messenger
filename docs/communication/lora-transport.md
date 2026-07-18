# LoRa Transport

The LoRa Transport provides long-range mesh capabilities. It operates by utilizing the paired ESP32 node as a bridge.

---

## 1. LoRa Radio Configuration

The SX1278 RA-02 transceiver is configured to balance transmission speed, range, and obstacle penetration:

| Parameter | Configuration | Detail |
|---|---|---|
| **Center Frequency** | 433.0 MHz | Permitted ISM band in most humanitarian zones. |
| **Spreading Factor (SF)** | 10 | Optimized for long range with acceptable airtime (~976 bps). |
| **Bandwidth (BW)** | 125 kHz | Standard signal bandwidth. |
| **Coding Rate (CR)** | 4/5 | Forward Error Correction (FEC) configuration. |
| **Sync Word** | `0xF3` | Prevents interference with commercial/public networks. |
| **Output Power** | +17 dBm | Limits current peak while maximizing signal envelope. |

---

## 2. Multi-Hop Mesh Routing

Because LoRa is limited to Line-of-Sight (LoS) or obstacle-degraded paths, the system operates as a **peer-to-peer relay network**:
* **Every Node acts as a Relay**: Incoming packets not addressed to the receiving node are processed and rebroadcast.
* **Seen Cache Deduplication**: To prevent loop floods, every node keeps a 128-entry circular buffer containing hashes of recently seen `sender_id + message_id` combinations.
* **Time-to-Live (TTL)**: Each packet starts with a set hop counter (default: `5`). The counter decrements at each hop. Packets reaching `TTL = 0` are silently discarded.

---

## 3. Link Budget & Signal Analysis

Each received packet is tagged with physical network metrics:
* **RSSI (Received Signal Strength Indicator)**: Measures absolute signal strength (typically -60 dBm to -125 dBm).
* **SNR (Signal-to-Noise Ratio)**: Measures signal strength relative to noise floor (+10 dB to -20 dB).

These metrics are transmitted to the Android application to feed the network dashboard interface.
