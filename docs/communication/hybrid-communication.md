# Hybrid Communication

The system relies on hybrid communication to ensure message delivery. It switches between high-speed short-range local links and long-range mesh radio networks.

---

## 1. Transport Priority Flow

The application's `CommunicationManager` operates as a state machine. It selects the transport pathway automatically:

```
                  Message Outbound
                         │
                         ▼
             +───────────────────────+
             │  Bluetooth Available? │
             +───────────┬───────────+
                         │
             ┌───────────┴───────────┐
            YES                      NO
             │                       │
             ▼                       ▼
    +─────────────────+    +───────────────────+
    │ Send via BLE    │    │  LoRa Mesh Node   │
    │ (Direct link)   │    │  Active & Known?  │
    +─────────────────+    +─────────┬─────────+
                                     │
                         ┌───────────┴───────────┐
                        YES                      NO
                         │                       │
                         ▼                       ▼
                +─────────────────+    +───────────────────+
                │ Relayed via LoRa│    │  Store & Forward  │
                │ (Mesh transport)│    │  (Offline Queue)  │
                +─────────────────+    +───────────────────+
```

---

## 2. Selection Criteria Rules

| Step | Transport Method | Selection Metric | Action |
|---|---|---|---|
| **1** | **Bluetooth** | Proximity check: Is the recipient's Node ID appearing on the active BLE device scan? | Write directly to GATT TX. Fast delivery, minimal airtime. |
| **2** | **LoRa Mesh** | Proximity check fails, but node is in local routing table (recently seen via HELLO). | Relayed over the 433MHz mesh network. Multi-hop delivery. |
| **3** | **Store & Forward** | Recipient is offline or out of mesh coverage area. | Packet is queued locally. Delivery is retried once a HELLO packet from the target node is received. |

---

## 3. Transparency to the User

Users do not need to check connectivity bars or toggle transports. The UI displays simple status alerts (Queued, Sent, Delivered) while the Communication Manager performs checks in the background.
