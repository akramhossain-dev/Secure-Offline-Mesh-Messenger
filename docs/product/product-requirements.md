# Product Requirements Document

**Project:** Offline Emergency Mesh Communication System  
**Version:** 1.0  
**Status:** Active Development  

---

## Project Goal

Build a fully self-contained, infrastructure-free communication platform for emergency and remote scenarios. The system enables two-way text, voice, and emergency messaging between Android devices using a combination of Bluetooth BLE (nearby) and LoRa 433MHz mesh radio (long-range), without any dependency on internet, cellular, or Wi-Fi infrastructure.

---

## Problem Statement

Modern communication infrastructure has a critical single point of failure: centralized networks. When cellular towers fail, internet exchange points go offline, or governments shut down carrier infrastructure, all digital communication ceases — precisely when people need it most.

During earthquakes, floods, civil emergencies, and remote field operations:
- Cell towers are damaged or overloaded
- Internet connectivity is severed
- Wi-Fi coverage is absent
- Commercial satellite systems are inaccessible to general users

This system fills that gap with a deployable, low-cost, decentralized communication mesh that works in any environment.

---

## Why Offline Communication Is Needed

| Scenario | Impact Without Offline Comms |
|---|---|
| Natural disaster | Survivors cannot call for help; rescue coordination fails |
| Government network shutdown | Legitimate communication becomes impossible |
| Remote operations | Teams operating beyond cellular have no fallback |
| Refugee and humanitarian camps | No infrastructure available for coordination |
| Search and rescue | Team members cannot coordinate in field conditions |

A system that operates independently of infrastructure is not a luxury — it is a life-safety requirement.

---

## Target Users

### 1. Rescue Teams
Professional first responders, search and rescue units, and disaster response organizations. They require:
- Reliable private channels within the team
- Emergency broadcast reception from affected populations
- Location sharing between team members
- Command coordination over distances of several kilometers

### 2. Disaster-Affected Populations
Civilians in earthquake, flood, or fire disaster zones. They require:
- Simple, one-tap SOS activation
- Ability to broadcast location without technical knowledge
- Communication with nearby survivors and rescue units

### 3. Remote Location Workers
Field researchers, mountain guides, survey teams, military units, and humanitarian workers operating in areas without cellular coverage. They require:
- Point-to-point messaging across 1–5 km
- Multi-hop relay to extend coverage across a deployment area
- Voice messages for environments where typing is impractical

### 4. Emergency Coordination Groups
Incident commanders, NGO coordinators, and community leaders managing emergency response. They require:
- Global broadcast to all nodes in the mesh
- Resource sharing and coordination tools
- Network dashboard to monitor node status

---

## Core Objectives

| Objective | Requirement |
|---|---|
| No internet dependency | Zero network calls; all data local |
| No SIM dependency | No cellular radio used |
| Offline communication | All features function without connectivity |
| Long-range messaging | LoRa 433MHz mesh covers 1–5 km per hop |
| Privacy-focused | No tracking, no cloud, user-controlled sharing |
| Emergency-first | SOS, broadcast, and location are first-class features |
| Low barrier to entry | Hardware buildable for approximately 2000 BDT per node |

---

## Functional Requirements

### Communication
- FR-01: App shall support private (point-to-point, E2E encrypted) text messaging
- FR-02: App shall support global (broadcast, unencrypted) mesh chat
- FR-03: App shall support voice messages (chunked audio transmission)
- FR-04: App shall support emergency broadcast with full-screen alert on receivers

### Transport
- FR-05: App shall use Bluetooth BLE for nearby device communication (priority 1)
- FR-06: App shall use LoRa mesh (via ESP32 bridge) for long-range communication (priority 2)
- FR-07: App shall cache undelivered messages and retry via Store & Forward (priority 3)
- FR-08: Transport selection shall be automatic — never exposed to the user

### Emergency
- FR-09: App shall provide an in-app SOS button with two-step confirmation
- FR-10: SOS shall embed phone GPS coordinates and repeat every 60 seconds until cancelled
- FR-11: App shall support Emergency Status flags (Normal / Emergency / Rescue / Coordinator)
- FR-12: SOS packets shall have Critical priority and bypass all queue ordering

### User & Identity
- FR-13: App shall generate a UUID Node ID at first launch (locally, no server)
- FR-14: App shall support QR code pairing for contact and key exchange
- FR-15: App shall provide offline contact management
- FR-16: App shall display nearby discoverable nodes via BLE scan

### Map & Location
- FR-17: App shall display contacts with active location shares on an offline map
- FR-18: Location sharing shall be explicit, user-initiated, and time-limited
- FR-19: No location shall be transmitted without user action

### Network & Routing
- FR-20: Each ESP32 node shall participate in mesh routing
- FR-21: Packet TTL (default: 5 hops) shall prevent routing loops
- FR-22: A seen-packet cache shall prevent duplicate processing
- FR-23: Store & Forward shall persist across app restarts via Room Database

### Monitoring
- FR-24: App shall display real-time power telemetry from the INA219 sensor
- FR-25: App shall display network dashboard (active nodes, transport status, queue depth)

---

## Non-Functional Requirements

| Category | Requirement |
|---|---|
| Performance | Message latency (1-hop BLE) < 500 ms |
| Performance | BLE connection established < 3 seconds |
| Reliability | LoRa range ≥ 1 km in open field |
| Reliability | Store & Forward delivers message on reconnection |
| Power | Node active current < 200 mA |
| Power | Node sleep current < 20 mA |
| Scalability | Architecture supports 50 / 100 / 1000+ node clusters |
| Security | All private messages encrypted with AES-256-GCM |
| Privacy | No background location access |
| Usability | SOS activatable within 3 taps from any screen |

---

## Success Criteria

| Criterion | Measurement |
|---|---|
| Message delivery | > 99% delivery rate within BLE range |
| Message delivery | > 95% delivery rate over 3-hop LoRa mesh |
| Network stability | Mesh recovers from single-node failure without user intervention |
| Low power usage | Node sustains ≥ 24-hour operation on 5000 mAh power bank |
| Scalable nodes | Cluster-based architecture validated at 50 / 100 / 1000 simulated nodes |
| SOS usability | SOS activated and first broadcast within 10 seconds of decision |

---

## Out of Scope

| Item | Reason |
|---|---|
| iOS application | Android-first; iOS deferred to future version |
| Cloud backend | Contradicts offline-first design principle |
| External GPS module | Android device GPS is sufficient |
| Battery charging circuit | Power Bank used as-is; no custom power hardware |
| Hardware SOS button | In-app SOS button preferred for simplicity |
| Satellite communication | Out of cost and complexity range |

---

## Related Documents

- [Use Cases](use-cases.md)
- [User Flow](user-flow.md)
- [System Architecture](../overview/system-architecture.md)
- [Development Roadmap](../roadmap/development-roadmap.md)
