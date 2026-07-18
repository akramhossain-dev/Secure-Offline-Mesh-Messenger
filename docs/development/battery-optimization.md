# Battery Optimization & Power Management — Phase A19

## Power Management Architecture

The Power Management subsystem tracks client hardware energy metrics offline, automatically toggling optimization profiles to prolong device execution:

```
 ┌───────────────────────────┐
 │      Battery Provider     │  (Abstracts Android OS / external INA219 chips)
 └─────────────┬─────────────┘
               │ BatteryModel
 ┌─────────────▼─────────────┐
 │       Power Manager       │  (Evaluates level thresholds and emits events)
 └─────────────┬─────────────┘
               ├───────────────────────┐
 ┌─────────────▼─────────────┐   ┌─────▼─────────────────────┐
 │   PowerSavingMode Flow    │   │     PowerEvent Warnings   │
 └───────────────────────────┘   └───────────────────────────┘
```

The system publishes metrics reactively via state flows monitored by communication queues, BLE discovery loops, and UI managers.

---

## Power Optimization Profiles

Depending on current battery levels, the [`PowerManager`](../../android/app/src/main/java/com/mesh/emergency/core/power/PowerManager.kt) shifts the application through three modes:

| Mode | Battery Threshold | Behavior Rules |
|---|---|---|
| **`NORMAL`** | Plugged in OR > 20% | Full scan and sync queues are active. standard BLE peripheral search intervals. |
| **`SAVING`** | 11% to 20% | Reduces scan frequency sweeps by 50%. pauses profile avatar syncing loops. |
| **`EMERGENCY`** | 0% to 10% | Pauses standard text/chat queues entirely. processes strictly `CRITICAL` SOS alerts. |

---

## Battery Telemetry Schema

The [`BatteryModel`](../../android/app/src/main/java/com/mesh/emergency/core/power/BatteryModel.kt) encapsulates:
- **Battery Level**: Integer percentage status (0-100).
- **Charging Status**: Boolean flag indicating if plugged into a power source.
- **Power Source**: String tag representing charging type (`ac`, `usb`, `wireless`, `battery`).
- **Health Status**: Battery wear diagnostics (`good`, `overheat`, `dead`, `overvoltage`).
- **Temperature Reference**: Celsius reading (critical to check when operating devices inside hot emergency zones).

---

## Future INA219 Current Sensor Integration

For external portable mesh repeaters, battery parameters cannot be queried from standard Android system managers. The architecture exposes the [`Ina219Sensor`](../../android/app/src/main/java/com/mesh/emergency/core/power/sensor/Ina219Sensor.kt) interface to allow custom I2C drivers to map:
- Shunt bus voltage readings.
- Milliamperes discharge currents.
- Instantaneous power usage ratings (Watts).
