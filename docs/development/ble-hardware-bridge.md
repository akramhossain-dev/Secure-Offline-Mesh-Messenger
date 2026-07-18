# BLE Communication & Hardware Bridge — Phase A30 + A31

## BLE Architecture (A30.1)

```
┌──────────────────────────────────────────────────────────┐
│                  Presentation Layer                      │
│   HardwareScreen ←→ HardwareViewModel (MVI)             │
└──────────────────────────────┬───────────────────────────┘
                               ↓
┌──────────────────────────────────────────────────────────┐
│               HardwareManager (A30.8)                    │
│  startDiscovery() · connectToDevice() · sendCommand()    │
│  refreshHardwareStatus() · connectedProfile: StateFlow   │
└──────────────────┬───────────────────────┬───────────────┘
                   ↓                       ↓
┌──────────────────────────┐  ┌───────────────────────────┐
│  BleDeviceRepository     │  │  Esp32CommandProtocol     │
│  (A30.3 / A30.5 / A30.6) │  │  (A31.2 / A31.3)          │
│  BleDeviceRepositoryImpl │  │  encode() / decode()      │
│  · LE Scanner (UUID FF10)│  │  Binary wire protocol     │
│  · ConcurrentHashMap     │  └───────────────────────────┘
│  · 15s auto-stop timeout │
└──────────────────┬───────┘
                   ↓
┌──────────────────────────────────────────────────────────┐
│           BluetoothTransportImpl (Phase A12)             │
│  GATT connect · write RX char · notify TX char          │
│  Service: 0000FF10 · RX: FF11 · TX: FF12               │
└──────────────────────────────────────────────────────────┘
```

---

## Transport Abstraction (A30.2)

The `Transport` interface (Phase A12) is the lowest-level abstraction:

| Implementation | Phase | Status |
|---|---|---|
| `MockTransport` | A12 | ✅ Loopback test transport |
| `BluetoothTransportStub` | A12 | ✅ Placeholder stub |
| `BluetoothTransportImpl` | A12 | ✅ Real GATT via Android BLE API |
| `LoRaTransportStub` | A12 | ✅ Placeholder stub |
| `WiFiTransportImpl` | Future A32 | ⏳ Not yet implemented |

`CommunicationManager` selects among transports by priority: BLE > LoRa > Mock.

---

## BLE Device Model (A30.4)

```kotlin
data class BleDevice(
    val id: String,              // MAC address
    val name: String,
    val macAddress: String,
    val rssi: Int,               // dBm — used for signal strength sorting
    val connectionState: BleConnectionState,
    val lastSeenMs: Long,
    val isCompatible: Boolean,   // advertises Mesh Service UUID
    val services: List<String>   // discovered GATT service UUIDs
)
```

### Connection States

```
DISCOVERED → CONNECTING → CONNECTED
                              ↓
                        DISCONNECTING → DISCONNECTED
CONNECTING → FAILED
```

---

## BLE Discovery (A30.5)

**Service UUID filter**: `0000FF10-0000-1000-8000-00805F9B34FB`  
Only ESP32 nodes advertising this UUID appear in the device list.

| State | Trigger |
|---|---|
| `IDLE` | Initial / after stop |
| `SCANNING` | `startDiscovery()` called |
| `STOPPED` | `stopDiscovery()` or 15s auto-timeout |
| `PERMISSION_DENIED` | Missing `BLUETOOTH_SCAN` permission |
| `BLE_UNAVAILABLE` | Adapter disabled / null |

**Battery protection**: Scan auto-stops after 15 000 ms via `delay` + coroutine.

---

## Hardware Bridge Architecture (A30.8 / A31.1)

### HardwareDeviceProfile

```kotlin
data class HardwareDeviceProfile(
    val hardwareId: String,
    val deviceType: HardwareDeviceType,   // ESP32_LORA | ESP32_ONLY | GENERIC_BLE | UNKNOWN
    val firmwareVersion: String,
    val batteryPercent: Int,
    val capabilities: Set<HardwareCapability>,  // LORA_TX | LORA_RX | GPS | BATTERY_MONITOR | MESH_RELAY
    val signalRssi: Int,
    val isConnected: Boolean,
    val lastPingMs: Long
)
```

Profile is built from decoded `Esp32Response.HardwareStatus` immediately after connection, then updated every 30 seconds by the background polling loop.

---

## ESP32 Command Protocol (A31.2 / A31.3)

### Wire Format

```
┌──────────┬──────────┬──────────┬────────────────────────────┐
│ CMD (1B) │ LEN_HI   │ LEN_LO   │ PAYLOAD (variable bytes)   │
└──────────┴──────────┴──────────┴────────────────────────────┘
```

### Commands

| Command | Opcode | Payload |
|---|---|---|
| `GetStatus` | `0x01` | — |
| `SendMessage` | `0x02` | `[ID_LEN][RECIPIENT_ID][MESSAGE_BODY]` |
| `ReceiveMessage` | `0x03` | — |
| `GetBattery` | `0x04` | — |
| `GetSignal` | `0x05` | — |
| `Reset` | `0x06` | — |
| `GetFirmwareVersion` | `0x07` | — |
| `ConfigureLoRa` | `0x08` | `[FREQ_4B][SF:1][BW:1]` |

### Responses (from ESP32 → Android)

| Response Code | Type | Key Fields |
|---|---|---|
| `0x81` | `HardwareStatus` | battery%, rssi, snr, fw, loraReady |
| `0x82` | `MessageSent` | success bool |
| `0x83` | `InboundMessage` | senderId, payload |
| `0x84` | `BatteryStatus` | voltageMilliV, percent |
| `0x85` | `SignalStatus` | rssiDbm, snrDb |
| `0x87` | `FirmwareVersion` | version string |
| `0xFF` | `Error` | error message |

---

## Packet Bridge Flow (A31.3)

```
App (Kotlin)                  BLE                  ESP32 (C++)
─────────────                 ───                  ───────────
Esp32Command.SendMessage
        ↓
Esp32CommandProtocol.encode()
        ↓ ByteArray
BluetoothTransportImpl.send()
        ↓ GATT write (FF11)
                              ──────────────────→
                                                  Handle 0x02
                                                  LoRa TX
                              ←──────────────────
                              GATT notify (FF12)
_receiveFlow.tryEmit()
        ↓ BleDataPacket
Esp32CommandProtocol.decode()
        ↓ Esp32Response.MessageSent
HardwareManagerImpl.handleResponse()
        ↓
Update connectedProfile StateFlow
```

---

## Error Handling (A31.5)

| Error Scenario | Handler |
|---|---|
| BLE unavailable | `_discoveryState = BLE_UNAVAILABLE` |
| Permission denied | `_discoveryState = PERMISSION_DENIED` |
| Device not found | `BleConnectionResult.Failure(DEVICE_NOT_FOUND)` |
| GATT service missing | `BleConnectionState.FAILED` |
| Connection timeout | Auto-cancel after 10 s in `BluetoothTransportImpl` |
| Invalid packet | `Esp32Response.Unknown` / `Malformed` |
| No device connected | `HardwareCommandResult.NoDeviceConnected` |

---

## Background Operation (A31.6)

- `HardwareManagerImpl` runs a `SupervisorJob`-backed `CoroutineScope(Dispatchers.IO)`
- **Status poll**: every 30 s when `connectedMac != null`
- **Scan auto-stop**: `BleDeviceRepositoryImpl` launches a coroutine that calls `stopDiscovery()` after 15 s
- All flows survive configuration changes as `@Singleton` Hilt-injected instances

---

## Test Coverage (A31.7)

[`BleHardwareTest.kt`](../../android/app/src/test/java/com/mesh/emergency/BleHardwareTest.kt) — 34 pure JVM tests:

- BLE device model + RSSI sorting
- Discovery state completeness
- Connection result sealed class
- All 8 command opcodes encoded correctly
- Response decoding: HardwareStatus, MessageSent, BatteryStatus, SignalStatus, FirmwareVersion, Error, Unknown
- Hardware profile capability checks
- BleDataPacket equality

---

## Phase A32 Readiness

| Capability | Ready For A32? |
|---|---|
| BLE scan + connect | ✅ |
| GATT characteristic write (RX) | ✅ |
| GATT notification read (TX) | ✅ |
| ESP32 command protocol | ✅ |
| LoRa config command | ✅ |
| WiFi transport slot | ⏳ |
| Multi-hop mesh routing | ⏳ |
| GPS integration | ⏳ |
