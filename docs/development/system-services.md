# Permission & System Services Foundation — Phase A7

## Permission Architecture

To meet user privacy controls and Material Design guidelines, permissions are tracked programmatically using semantic type classifications:

- **Permission Manager**: [`PermissionManager`](../../android/app/src/main/java/com/mesh/emergency/core/utils/PermissionManager.kt) handles runtime permission verification.
- **States**: [`PermissionState`](../../android/app/src/main/java/com/mesh/emergency/core/utils/permission/PermissionState.kt) maps values as `Granted`, `Denied`, `PermanentlyDenied` (requiring Settings routing), or `NotRequested`.
- **Feature Categories**: [`PermissionType`](../../android/app/src/main/java/com/mesh/emergency/core/utils/permission/PermissionType.kt) wraps underlying Android SDK string lists:

| Category | Platform Permissions | Min SDK / API Version rules |
|---|---|---|
| `BLUETOOTH` | `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`, `BLUETOOTH_ADVERTISE` | Pre-Android 12 uses legacy bluetooth admin permissions. |
| `LOCATION` | `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION` | Required to scan for nearby BLE peripherals. |
| `MICROPHONE` | `RECORD_AUDIO` | Reserved for emergency voice broadcasts. |
| `NOTIFICATION`| `POST_NOTIFICATIONS` | Android 13+ (API 33) enforces notifications post permission. |

---

## Device Capabilities Management

To prevent crashes on older phones or devices without transceivers (e.g. tablet variants lacking Bluetooth), capability checks are audited before launching features:

- **Capability Auditor**: [`DeviceCapabilityManager`](../../android/app/src/main/java/com/mesh/emergency/core/utils/capability/DeviceCapabilityManager.kt)
- **Features Checked**:
  - `isBluetoothSupported()`: BLE hardware verification.
  - `isLocationSupported()`: GPS hardware checking.
  - `isMicrophoneSupported()`: Audio input hardware checking.
  - `isGpsEnabled()`: Inspects if location services are toggled on in the system tray.

---

## Platform Service Wrappers

To enforce **SOLID design principles** and support mock testing on JVM environments (without launching the Android Emulator), system services are isolated:

- **Bluetooth**: [`BluetoothServiceWrapper`](../../android/app/src/main/java/com/mesh/emergency/core/system/BluetoothServiceWrapper.kt) wraps adapter state checks.
- **GPS Location**: [`LocationServiceWrapper`](../../android/app/src/main/java/com/mesh/emergency/core/system/LocationServiceWrapper.kt) checks coordinates services provider.
- **Audio Manager**: [`AudioServiceWrapper`](../../android/app/src/main/java/com/mesh/emergency/core/system/AudioServiceWrapper.kt) monitors mic mute settings.
- **Notifications**: [`NotificationServiceWrapper`](../../android/app/src/main/java/com/mesh/emergency/core/system/NotificationServiceWrapper.kt) manages alerts delivery.
- **DI Provisioning**: All bindings are registered in the Hilt [`UtilityModule`](../../android/app/src/main/java/com/mesh/emergency/di/UtilityModule.kt).
