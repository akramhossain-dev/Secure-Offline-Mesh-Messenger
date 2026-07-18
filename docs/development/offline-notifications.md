# Offline Notification & Alert System — Phase A21

## Notification Architecture

The Notification subsystem coordinates displaying important alerts offline (without internet connection or push servers like FCM) to warn users about emergency events, low battery thresholds, and incoming message packets:

```
 ┌───────────────────────────┐
 │   Alert Trigger Sources   │  (Emergency / Power / Comms Managers)
 └─────────────┬─────────────┘
               │ AlertModel
 ┌─────────────▼─────────────┐
 │   Notification Manager    │  (Filters priorities and checks preferences)
 └─────────────┬─────────────┘
               │
 ┌─────────────▼─────────────┐
 │   Notification Channels   │  (System notification categories)
 └─────────────┬─────────────┘
               │
 ┌─────────────▼─────────────┐
 │ Android Notification Sys  │  (Renders alerts on lock screen / banner)
 └───────────────────────────┘
```

The system publishes alerts reactively via the [`NotificationManager`](../../android/app/src/main/java/com/mesh/emergency/core/notification/NotificationManager.kt) contract.

---

## Alert Priority Policy

System notifications are routed to specific system channels based on priority levels:

- **`CRITICAL` (SOS Alerts)**:
  - Channel: `EMERGENCY`.
  - Behavior: Bypasses standard system sound/vibration overrides, playing audible alerts and vibrating continuously to draw immediate user attention.
- **`HIGH` (Battery warnings)**:
  - Channel: `POWER`.
  - Behavior: High-priority banner warning about battery critical conditions.
- **`NORMAL` (Direct chat)**:
  - Channel: `MESSAGE`.
  - Behavior: Standard chat notification.
- **`LOW` (Background sync status)**:
  - Channel: `SYSTEM`.
  - Behavior: Silent background diagnostics status update indicators.

---

## Alert Data Schema

The [`AlertModel`](../../android/app/src/main/java/com/mesh/emergency/core/notification/AlertModel.kt) encapsulates:
- **Alert ID**: Unique identification string.
- **Type**: Category identifier (`SOS_ALERT`, `MESSAGE_ALERT`, `NODE_ALERT`, `BATTERY_ALERT`, `SYSTEM_ALERT`).
- **Title / Description**: Text headers shown to the user.
- **Priority**: Priority ranking (`CRITICAL`, `HIGH`, `NORMAL`, `LOW`).
- **Source**: Manager origin description string.
- **Status**: Diagnostic tracking state.

---

## User Control & Notification Preferences

To allow users to conserve battery and keep privacy:
- **Category Enable/Disable**: Users can silence channels like `NETWORK` and `SYSTEM` completely.
- **Sound & Vibration**: Toggles sound/vibration markers per channel.
- **Emergency Override**: The `EMERGENCY` channel overrides preferences to always play audible alerts when a critical SOS is received, ensuring safety alerts are never missed.
