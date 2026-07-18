# Offline Analytics, Logging & Debug System — Phase A23

## Logging Architecture

The diagnostics logging subsystem collects and stores application events locally inside the SQLite database, completely detached from cloud telemetry networks:

```
 ┌───────────────────────────┐
 │       Logger Manager      │  (Filters log levels and toggles debug rules)
 └─────────────┬─────────────┘
               │ LogEntity
 ┌─────────────▼─────────────┐
 │    Local SQLite Database  │  (Persists logs on local disk)
 └─────────────┬─────────────┘
               │
 ┌─────────────▼─────────────┐
 │    Diagnostic Export      │  (Formats lists to text files)
 └───────────────────────────┘
```

The system manages diagnostic logs using the [`LoggerManager`](../../android/app/src/main/java/com/mesh/emergency/core/log/LoggerManager.kt) wrapper.

---

## Log Levels & Categories

### 1. Log Levels
- **`DEBUG`**: Development details (filtered out in production release builds unless Debug Mode is enabled).
- **`INFO`**: Normal operations events.
- **`WARNING`**: Potential anomalies (e.g. high queue retries).
- **`ERROR`**: Caught exceptions.
- **`CRITICAL`**: Device system failures.

### 2. Log Categories
- `SYSTEM`: General application status.
- `NETWORK`: Mesh message transmissions.
- `BLUETOOTH`: GATT and adapter connection logs.
- `LORA`: SX1278 packet exchanges.
- `SECURITY`: Key derivation and decryption updates.
- `BATTERY`: Battery level shifts.
- `LOCATION`: Coordinates query loops.

---

## Local Diagnostic Storage Schema

The [`LogEntity`](../../android/app/src/main/java/com/mesh/emergency/data/local/entity/LogEntity.kt) tracks:
- **Log ID**: Unique key string.
- **Level / Category**: String enums representations.
- **Message**: Detailed descriptive message.
- **Timestamp**: Time epoch.
- **Device ID**: Originating client identifier.
- **Module Name**: Specific class or service log source descriptor.
- **Stack Trace**: Formatted Exception stack logs (critical for resolving offline crashes).

---

## Privacy Policy & User Control

To preserve operator security:
1. **Zero Personal Identifiable Info (PII)**: The logger completely filters out user profile nicknames, text message contents, or private location coordinates from log strings.
2. **Local Scope**: Logs never attempt to sync with remote tracking APIs.
3. **Storage Clears**: Users can select "Clear History" inside settings to purge SQLite `logs` rows instantly.
4. **Diagnostic Export**: Logs are formatted into a single file payload, which can be shared manually by the operator to support developers troubleshooting mesh failures.
