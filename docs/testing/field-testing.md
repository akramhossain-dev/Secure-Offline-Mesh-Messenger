# Field Testing

This document details the field testing protocols used to validate hardware range, signal quality, and power consumption in outdoor conditions.

---

## 1. Range Testing Protocols

Range testing is conducted in three test environments using direct point-to-point links (SF10, 433 MHz, 17 dBm):

| Environment | Target Distance | Verification Metrics |
|---|---|---|
| **Urban Environment** | 500 m to 1.5 km | Measures building penetration and signal degradation. |
| **Suburban Environment** | 1.5 km to 3 km | Measures signal attenuation in suburban environments. |
| **Open Field (LoS)** | 3 km to 5 km+ | Baseline range test under line-of-sight conditions. |

### Test Method:
1. Node A remains stationary at a height of 1.5 meters.
2. Node B is moved to pre-determined coordinates using GPS coordinates on the mobile map.
3. Node B sends test packets, recording received RSSI and SNR values.

---

## 2. Multi-Node Mesh Testing

To validate routing behavior in real-world conditions, field tests are conducted with multiple physical nodes:
* **Node Count**: 5 physical nodes placed in a linear grid.
* **Test Case**: Node 1 sends a message to Node 5, requiring multi-hop relay.
* **Verification**:
  * Check serial console logs to verify path traversal: `Node 1 -> Node 2 -> Node 3 -> Node 4 -> Node 5`.
  * Confirm that intermediate nodes decrement the TTL correctly.
  * Verify that the seen-packet cache drops duplicate transmissions.

---

## 3. Power Consumption Verification

* **Telemetry Verification**: Compare INA219 current draw readings on the mobile app with a calibrated inline USB power meter.
* **Active Mode Test**: Measure current draw in active BLE + LoRa receive mode to verify it remains under the **200 mA** design limit.
* **Power Saving Test**: Disconnect the mobile companion and verify that the ESP32 node enters light sleep, reducing current draw to under **20 mA**.
* **Battery Life Test**: Run the node continuously on a 5000 mAh power bank to verify it meets the 24-hour runtime goal.
