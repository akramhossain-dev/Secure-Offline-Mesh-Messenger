# Theme System, UI Foundation & Navigation Shell Overview — Phase A4

## Design Principles

The UI of the Offline Emergency Mesh Communication System has been custom designed around key principles suitable for **outdoor usage, high legibility, and low power consumption**:

- **Simple & Minimal**: Avoid clutter or flashy details. Focus on speed of action in stressful crisis scenarios.
- **Accessible & High Contrast**: Large fonts and strong visual accents to support viewing in bright outdoor sunlight.
- **Battery Friendly**: Dark theme maps dark slate gray and deep black backgrounds to reduce display power draw on AMOLED screens.
- **Subtle Glassmorphism**: Utilized in dashboard components to layer status nodes without visual clutter.
- **Aurora Accents**: Gradual backdrop glows indicating the active mesh signal fields in settings and network boards.
- **No Fancy Animations**: Animation transitions are restricted to fade, scale, or sliding progress to keep frame renders fast and save CPU cycles.

---

## Semantic Color System

Beyond the standard Material Design 3 palettes, the system has a custom CompositionLocal provider `LocalSemanticColors` delivering unified status color tokens:

| Property | Color | Context |
|---|---|---|
| `connected` | Teal50 (`#008C96`) | Active BLE link / Peripheral synced |
| `offline` | Neutral50 (`#777680`) | Device disconnected / Offline standby state |
| `weakSignal` | Amber60 (`#B77600`) | Poor RSSI levels / Battery charge warnings |
| `strongSignal` | Teal60 (`#00A8B4`) | High quality RSSI values |
| `emergency` | Red50 (`#DE3730`) | Active SOS beacon broadcasts / Critical failures |
| `warning` | Amber70 (`#DA8E00`) | Out-of-bounds metrics alerts |
| `success` | Teal60 (`#00A8B4`) | Transmissions acknowledged |
| `info` | Indigo50 (`#4F4BD4`) | System info banners |
| `disabled` | Neutral80 (`#C7C5D0`) | Inactive controls |
| `outline` | NeutralVar50 (`#767682`)| Translucent divider board boundaries |

Usage in Compose code:
```kotlin
val colors = MeshThemeTokens.semanticColors
val indicatorColor = if (isConnected) colors.connected else colors.offline
```

---

## Spacing & Dimensions Tokens

A strict 4dp base layout grid is defined in [`Spacing.kt`](../../android/app/src/main/java/com/mesh/emergency/core/designsystem/theme/Spacing.kt). Component paddings and layouts must consume these tokens:

| Token | Dimension | Use Case |
|---|---|---|
| `none` | 0.dp | Reset parameters |
| `xxs` | 2.dp | Minor offsets |
| `xs` | 4.dp | Text padding / inner badges offsets |
| `sm` | 8.dp | Inner chips layout spacing / gap between rows |
| `md` | 12.dp | Standard list item vertical spacing |
| `lg` | 16.dp | Screen margins / Card contents margins |
| `xl` | 20.dp | Large category offsets |
| `xxl` | 24.dp | Bottom sheet vertical separators |
| `xxxl`| 32.dp | Form fields groupings spacer |
| `huge`| 40.dp | Splash margins |
| `massive`| 48.dp| Minimum tap targets spacing |
| `giant`| 64.dp | FAB margins / Header layouts offset |

---

## Shape System

Corner radii tokens map as follows in [`Shape.kt`](../../android/app/src/main/java/com/mesh/emergency/core/designsystem/theme/Shape.kt):
- `extraSmall` (4dp) — Badges / Chips
- `small` (8dp) — Buttons / Action inputs
- `medium` (12dp) — Cards / Dialogs / Details sheets
- `large` (16dp) — Bottom panels
- `extraLarge` (28dp) — Modals / Bottom sheets overlay

---

## Icon Architecture

Standardized in [`MeshIcons.kt`](../../android/app/src/main/java/com/mesh/emergency/core/designsystem/icon/MeshIcons.kt), icons are categorized logically:
- **Navigation**: `Back`, `Menu`, `Close`, `Home`, `Settings`
- **Communication**: `Chat`, `Send`, `Voice`, `Global`, `QrScan`
- **Emergency**: `Emergency`, `Warning`, `Info`, `Alert`
- **Network**: `Bluetooth`, `BluetoothDisabled`, `SignalStrong`, `SignalWeak`, `SignalNone`, `NetworkCheck`
- **Battery**: `BatteryFull`, `BatteryCharging`, `BatteryLow`, `BatteryUnknown`
- **Location**: `Location`, `Map`, `MyLocation`

---

## Navigation & App Shell Architecture

Phase A4 implements the complete application navigation layout scaffolding:

### 1. Adaptive Navigation Shell (`AppShell.kt`)
The UI shell dynamically updates layout formats matching window widths using the `rememberWindowSize` helper:
- **Compact Window (Phone, < 600dp)**: Standard Bottom Navigation Bar with screen navigation targets.
- **Medium & Expanded Window (Tablets/Landscape, >= 600dp)**: Displays a vertical `NavigationRail` alongside screen contents for optimal landscape real estate.
- **Gesture Control**: Modal sheets gesture boundaries are mapped to only allow swipes on top-level routes (e.g. Home, ChatList, Map, Diagnostics).

### 2. Navigation Actions Flow
ViewModels request routing changes through the `AppNavigator` singleton coordinator. The root `AppShell` collects navigation actions and dispatches updates to `NavController` safely:
- State restoration (`restoreState = true`, `saveState = true`) is configured to preserve scroll positions across layout jumps.
- Multiple button clicks are ignored using `launchSingleTop = true`.

### 3. Transition Animators (`NavTransitions.kt`)
All screen transitions follow standard Material 3 motion specs:
- **Forward Navigation**: 300ms Slide-in from right.
- **Backward/Pop Navigation**: 300ms Slide-out to right.

### 4. Deep Links
Deep links leverage standard schema patterns: `mesh://emergency/...`
- Chat Session: `mesh://emergency/chat/{contactId}`
- Emergency Activation: `mesh://emergency/sos`
- User Profile: `mesh://emergency/profile`
- Settings Screen: `mesh://emergency/settings`

---

## Accessibility Guidelines

- **Touch Targets**: Standard buttons occupy a minimum target height of `48dp`. The emergency `MeshSosButton` touch target is expanded to `72dp` to support fast finger targeting in panic situations.
- **Font Scaling**: All text styles consume `sp` scaling units mapping directly to system preference resizing.
- **Screen Reader Compatibility**: Image tags define clear content descriptions. New elements are layered correctly to avoid navigation traversal issues.
