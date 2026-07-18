# Communication Core Architecture — Phase A8

## Hybrid Communications Strategy

Responders operating in emergency areas require multiple transport options. The system isolates the user layer from physical network interfaces by routing messages through a central **Communication Manager**:

```
 ┌───────────────────────────┐
 │    Presentation / Chat    │
 └─────────────┬─────────────┘
               │ invokes UseCases
 ┌─────────────▼─────────────┐
 │   Communication Manager   │  ◄── (Dynamic Priority Selector)
 └─────────────┬─────────────┘
               ├───────────────────────┬───────────────────────┐
 ┌─────────────▼─────────────┐   ┌─────▼─────────────────────┐   ┌─────▼─────────────────────┐
 │    Bluetooth Transport    │   │      LoRa Transport       │   │      Mock / Loopback      │
 └───────────────────────────┘   └───────────────────────────┘   └───────────────────────────┘
```

---

## Transport Abstraction

The [`Transport`](../../android/app/src/main/java/com/mesh/emergency/core/communication/Transport.kt) interface exposes standard hardware interface controls:
- `connect()` / `disconnect()`: Bridge transceiver link states.
- `send(data: ByteArray)`: Transmit raw bytes.
- `receive()`: Expose a Flow streaming incoming raw bytes.
- `status`: Expose `TransportStatus` (`CONNECTED`, `DISCONNECTED`, `CONNECTING`, `UNAVAILABLE`) reactively.

---

## Active Channel Selection & Priorities

The system automatically switches pathways when a transceiver connects/disconnects using a strict prioritization hierarchy inside [`CommunicationManagerImpl`](../../android/app/src/main/java/com/mesh/emergency/data/communication/CommunicationManagerImpl.kt):

| Priority | Transport Type | Rationale |
|---|---|---|
| **1 (Highest)** | `BLUETOOTH` | Short range, high throughput, low latency. Preferred for local peer chats. |
| **2** | `LORA` | Long range, low throughput. Preferred when out of Bluetooth range. |
| **3** | `MOCK` | Loopback diagnostic channel used in emulator and test environments. |

### Selection Algorithm
1. Retrieve all registered transceivers in the `TransportRegistry` map.
2. Sort them by priority.
3. Select the first transceiver that is in a `CONNECTED` state.
4. If none are connected, fallback to the first in `CONNECTING` or `DISCONNECTED` state.
5. Set `activeTransport` and update the global `CommunicationState` StateFlow.

---

## Delivery Outcomes Tracking

Message deliveries wrap structured result states:
- `PENDING`: Enqueued in queue.
- `SENDING`: Actively writing to physical transceivers.
- `SENT`: Transmitted over the air.
- `DELIVERED`: Acknowledged by target node hop.
- `FAILED`: Timed out or write failure.
- `EXPIRED`: Packet time-to-live (TTL) exceeded before delivery succeeded.
