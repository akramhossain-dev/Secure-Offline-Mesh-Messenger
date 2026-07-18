# Project Overview

**Offline Emergency Mesh Communication System**

---

## Problem Statement

Modern communication systems are entirely dependent on centralized infrastructure — cellular towers, internet exchange points, and cloud services. When these fail — during earthquakes, floods, government shutdowns, or remote operations — all communication ceases.

This project addresses that gap by providing a self-contained, peer-to-peer communication network that operates independently of any infrastructure.

---

## Project Goals

| Goal | Description |
|---|---|
| Infrastructure independence | Operate without internet, SIM, or Wi-Fi |
| Long-range communication | Cover 1–5 km per node via LoRa 433MHz |
| Multi-hop mesh routing | Messages relay across multiple nodes automatically |
| Android user interface | Familiar mobile app for end users |
| Emergency-first design | SOS, location sharing, and broadcast capabilities |
| Low power consumption | Sustainable operation on a standard power bank |
| Open and extensible | Modular design for future hardware and feature expansion |

---

## Design Principles

### 1. Offline-First
Every feature functions without network connectivity. Data is stored locally on the Android device. No cloud dependency exists at any layer.

### 2. Resilient Mesh Topology
There is no single point of failure. Each ESP32 node acts as both a sender and a relay. If one node goes down, the mesh reroutes around it.

### 3. Emergency-Priority Messaging
SOS packets carry the highest routing priority and are forwarded by all nodes regardless of destination. Emergency broadcasts preempt all other traffic.

### 4. Hybrid Communication — Automatic Transport Selection
The Communication Manager inside the Android app selects the best available transport for every message without user involvement:

```
Bluetooth (nearby device, lowest latency)
    ↓  if not reachable
LoRa Mesh (long-range, multi-hop)
    ↓  if destination offline
Store & Forward (cache and retry on reconnection)
```

Users compose messages as normal. Transport decisions are invisible.

### 5. Privacy by Design
Location sharing is explicit and user-controlled. No background tracking. No persistent identifiers are exposed without user consent. End-to-end encryption is applied to all private messages.

### 6. Low Barrier to Deployment
A node consists of five components: an ESP32 board, an SX1278 LoRa module, a 433MHz Rubber Duck SMA antenna, a U.FL/SMA adapter cable, and a power bank. No specialized tools, soldering, or enclosures are required for basic deployment.

---

## Use Cases

### Natural Disaster Response
Earthquake or flood survivors use the app to broadcast SOS signals and GPS coordinates. Rescue teams with nodes deployed across the disaster zone relay messages to command centers without any cellular connection.

### Remote Field Operations
Field researchers, mountain rescue teams, or military units operating beyond cellular coverage deploy multiple nodes as a portable mesh backbone. The network self-organizes with no configuration required.

### Civil Emergency / Network Shutdown
During civil unrest or government-ordered network shutdowns, individuals can communicate locally and securely within the LoRa mesh range without relying on any infrastructure that can be disabled.

### Community Off-Grid Networks
Remote villages, off-grid communities, and humanitarian camps deploy fixed nodes as local communication infrastructure, enabling ongoing coordination, resource sharing, and emergency alerting.

---

## System Boundaries

| Included | Excluded |
|---|---|
| Offline BLE-to-LoRa bridge | Cloud sync or server component |
| Android messaging application | iOS application |
| Mesh routing and store-and-forward | Centralized dispatch systems |
| AES-encrypted private messaging | Certificate authority or PKI infrastructure |
| Power monitoring via INA219 | Battery management or charging circuits |
| GPS coordinate sharing (via phone built-in GPS) | External GPS hardware module |

---

## Hardware Philosophy

The hardware is deliberately minimal and commercially available:

- **ESP32 V1.3 Dev Board (CH340C)** — widely available, well-documented, dual-core processor
- **SX1278 RA-02 (433MHz)** — proven LoRa chipset, long range, low power
- **433MHz Rubber Duck SMA Antenna** — improved gain and signal stability
- **U.FL/IPEX to SMA Adapter Cable** — connects RA-02 to external SMA antenna
- **INA219** — precise power monitoring for device health tracking
- **Power Bank** — universally available, rechargeable, no custom power circuit needed

This approach ensures that nodes can be built by non-specialists, sourced globally, and replaced in the field without specialized components.

---

## Key Performance Targets

| Metric | Target |
|---|---|
| LoRa range (open field) | 3–5 km |
| LoRa range (urban) | 500 m – 1.5 km |
| Message latency (1-hop) | < 500 ms |
| BLE connection time | < 3 seconds |
| Node power consumption (active) | < 200 mA |
| Node power consumption (idle) | < 20 mA |
| Maximum mesh hops | 5 |
| Packet TTL (default) | 5 hops |
