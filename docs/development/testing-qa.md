# Testing Framework & Quality Assurance — Phase A24

## Testing Architecture

```
 ┌──────────────────────────────────┐
 │          Unit Tests              │  Pure JVM — no Android framework
 │  SecurityLayerTest               │  (AES, ECDH, SHA-256)
 │  MessageProtocolTest             │  (Packet serialization / TTL)
 │  StoreForwardTest                │  (Queue, retry, expiry)
 │  EmergencyManagerTest            │  (SOS distress lifecycle)
 │  LoggerSystemTest                │  (Log levels, debug toggle)
 │  NotificationSystemTest          │  (Priority routing, preferences)
 │  LocalizationSystemTest          │  (Language switch, format)
 │  PowerManagerTest                │  (Battery thresholds)
 │  ResourceManagerTest             │  (CRUD, match offers)
 │  LocationLayerTest               │  (State transitions)
 │  NetworkAwarenessTest            │  (Node tracking)
 │  VoiceManagerTest                │  (Recording, metadata)
 │  DeviceDiscoveryTest             │  (QR handshake)
 │  CommunicationManagerTest        │  (Transport selection)
 │  BluetoothTransportTest          │  (BLE stub dispatch)
 │  LoRaTransportSimulationTest     │  (LoRa mock TTL)
 │  SystemServiceTest               │  (Notification wrapper)
 └──────────────────────────────────┘
             ↓
 ┌──────────────────────────────────┐
 │       Integration Tests          │  Scenario-based multi-component
 │  OfflineScenarioTest             │  (13 adverse condition scenarios)
 │  TestDataFactoryTest             │  (Factory correctness verification)
 └──────────────────────────────────┘
```

---

## Test Support Infrastructure

Three shared test utilities are available in `test/com/mesh/emergency/test/`:

| Class | Purpose |
|---|---|
| [`TestDataFactory`](../../android/app/src/test/java/com/mesh/emergency/test/TestDataFactory.kt) | Builds valid domain entities with sensible defaults |
| [`MockFactory`](../../android/app/src/test/java/com/mesh/emergency/test/MockFactory.kt) | Creates hand-crafted stub implementations without Mockito verbosity |
| [`ScenarioSimulator`](../../android/app/src/test/java/com/mesh/emergency/test/ScenarioSimulator.kt) | Produces error `Result` values for adverse offline scenario testing |

---

## Coverage Goals

| Layer | Target |
|---|---|
| Core business logic | ≥ 80% |
| Security (crypto / keys) | ≥ 90% |
| Data layer (entities / DAOs) | ≥ 70% |
| Communication manager | ≥ 70% |

---

## Running Tests

```bash
# Run all unit tests
cd android && ./gradlew testDebugUnitTest

# Run a specific test class
./gradlew testDebugUnitTest --tests "com.mesh.emergency.SecurityLayerTest"

# Run all offline scenario tests
./gradlew testDebugUnitTest --tests "com.mesh.emergency.OfflineScenarioTest"

# Run with coverage report
./gradlew testDebugUnitTest jacocoTestReport
```

---

## CI Integration Notes

- All tests are pure JVM (`src/test/`) and execute without a connected device or emulator.
- `testOptions.unitTests.isReturnDefaultValues = true` in `build.gradle.kts` prevents Android SDK stub crashes.
- Test task `testDebugUnitTest` is the canonical CI gate before any release build.
- Mockito is available via `libs.bundles.testing.unit` for finer-grained mock control when needed.

---

## Offline Scenario Test Coverage

The `OfflineScenarioTest` suite covers 13 adverse conditions:

1. **BLE adapter disabled** — send returns error
2. **LoRa radio unavailable** — send returns error  
3. **Connection timeout** — timeout message validated
4. **Bluetooth permission denied** — `SecurityException` propagated
5. **Location permission denied** — `SecurityException` propagated
6. **Database write failure** — disk full error propagated
7. **Critical battery** — `PowerManager.isBatteryCritical()` returns true
8. **Full battery** — `PowerManager.isBatteryCritical()` returns false
9. **Message TTL expiry** — queue drop error returned
10. **Malformed packet** — deserialization error returned
11. **Duplicate packet** — routing table discard returned
12. **No internet — local notification** — still displays successfully
13. **Unknown locale** — language manager falls back to English
