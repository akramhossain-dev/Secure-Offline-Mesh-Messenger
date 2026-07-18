# Simulation Testing

This document details the simulation parameters used to validate the mesh routing engine at scale prior to field deployment.

---

## 1. Node Simulation Scaling

To ensure the mesh protocol remains stable as the network grows, routing behaviors are validated using simulated node matrices:

### A. Small Scale (50 Nodes)
* **Goal**: Validate simple multi-hop routing paths.
* **Test Case**: Random distribution within a 2x2 km simulated grid.
* **Metrics**: Path length verification, seen cache hit ratios, and duplicate packet drop rates.

### B. Medium Scale (100 Nodes)
* **Goal**: Verify routing performance under load.
* **Test Case**: Simulated grid with periodic hello broadcasts.
* **Metrics**: Packet collisions, latency distribution, and route recovery times.

### C. Regional Scale (1000+ Nodes)
* **Goal**: Validate cluster-based mesh boundaries.
* **Test Case**: Multi-cluster grid simulating independent regions bridged by Cluster Heads.
* **Metrics**: Memory footprint on Cluster Heads, packet delivery success rate, and route discoverability.

---

## 2. Simulation Environment Parameters

| Parameter | Configuration | Detail |
|---|---|---|
| **Propagation Model** | Log-distance Path Loss | Simulates path loss in mixed obstacle terrains. |
| **Packet Loss Rate (PLR)** | 1% to 15% (Dynamic) | Models link instability caused by distance and noise. |
| **Preamble Collision** | Enabled | Simulates packet loss when multiple nodes transmit simultaneously. |
| **Seen Cache Size** | 128 Entries | Validates seen-cache capacity under broadcast load. |

---

## 3. Key Simulation Verification Metrics

* **Time-on-Air (ToA) Impact**: Evaluates channel utilization to prevent broadcast storms.
* **Delivery Rate vs. Hop Count**: Measures E2E delivery rate over multiple relay hops.
* **Store & Forward Delivery Rate**: Validates that cached messages are delivered successfully when offline nodes reconnect.
