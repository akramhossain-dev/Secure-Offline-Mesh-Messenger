# Design System Guide — Phase A25

## Overview

The Offline Emergency Mesh design system is built on **Material 3** with an **Aurora UI + Glassmorphism** visual layer. It prioritises readability and emergency-context clarity above decoration.

---

## Theme Architecture

```
MeshTheme (root)
  ├── MaterialTheme  ← M3 ColorScheme + MeshTypography + MeshShapes
  ├── LocalSpacing   ← Spacing tokens (4dp grid)
  ├── LocalSemanticColors ← Semantic tokens (connected, offline, emergency…)
  └── LocalAuroraColors   ← Aurora glass tokens (glassSurface, auroraStart…)
```

### Theme Modes

| Mode | Behaviour |
|---|---|
| `ThemeMode.SYSTEM` | Follows Android OS dark/light setting (default) |
| `ThemeMode.DARK` | Always dark — recommended for outdoor/night use |
| `ThemeMode.LIGHT` | Always light |

Dynamic Color (Android 12+) is opt-in via `dynamicColor = true` parameter.

---

## Color System

### Brand Palette

| Role | Light | Dark | Usage |
|---|---|---|---|
| Primary | Indigo50 | Indigo80 | Buttons, active states |
| Secondary | Teal40 | Teal80 | Connection/signal |
| Tertiary | Amber80 | Amber80 | Warnings |
| Error | Red40 | Red80 | SOS, critical alerts |

### Semantic Tokens (via `LocalSemanticColors`)

| Token | Meaning |
|---|---|
| `connected` | Green/teal — BLE/LoRa link active |
| `offline` | Grey — no mesh node |
| `weakSignal` | Amber — RSSI below -70 dBm |
| `emergency` | Red — SOS / CRITICAL |
| `warning` | Amber — LOW battery / high queue |
| `success` | Teal — delivery confirmed |

### Aurora Glass Tokens (via `LocalAuroraColors`)

| Token | Value (Dark) | Usage |
|---|---|---|
| `glassBackground` | Indigo10 @ 80% | Screen backdrop panels |
| `glassSurface` | White @ 10% | Card/panel inner fill |
| `glassEmergency` | Red60 @ 20% | SOS panel tint |
| `glassWarning` | Amber70 @ 20% | Battery warning tint |
| `glassBorder` | White @ 20% | Inner rim border stroke |
| `auroraStart/Mid/End` | Indigo30→Teal30→Indigo20 | Backdrop gradient |

---

## Component Library

### Existing (Phase A1)
`MeshButton`, `MeshOutlinedButton`, `MeshTextButton`, `MeshIconButton`, `MeshSosButton`, `MeshCard`, `MeshBadge`, `MeshChip`, `MeshAppBar`, `MeshNavigationBar`, `MeshNavigationDrawer`, `MeshStatusIndicator`, `MeshBatteryStatus`, `MeshSignalIndicator`, `LoadingView`, `EmptyState`, `ErrorState`

### Added (Phase A25)

| Component | File | Purpose |
|---|---|---|
| `GlassPanel` | [GlassPanel.kt](../../android/app/src/main/java/com/mesh/emergency/core/designsystem/component/GlassPanel.kt) | Aurora glassmorphism surface |
| `AuroraBackdrop` | [GlassPanel.kt](../../android/app/src/main/java/com/mesh/emergency/core/designsystem/component/GlassPanel.kt) | Gradient screen background |
| `PriorityBadge` | [PriorityBadge.kt](../../android/app/src/main/java/com/mesh/emergency/core/designsystem/component/PriorityBadge.kt) | CRITICAL/HIGH/NORMAL/LOW chip |
| `NetworkStatusCard` | [EmergencyComponents.kt](../../android/app/src/main/java/com/mesh/emergency/core/designsystem/component/EmergencyComponents.kt) | Transport + node count card |
| `BatteryStatusCard` | [EmergencyComponents.kt](../../android/app/src/main/java/com/mesh/emergency/core/designsystem/component/EmergencyComponents.kt) | Battery level card |
| `SosIndicatorBanner` | [EmergencyComponents.kt](../../android/app/src/main/java/com/mesh/emergency/core/designsystem/component/EmergencyComponents.kt) | Active SOS banner |
| `UiStateBox` | [UiStateBox.kt](../../android/app/src/main/java/com/mesh/emergency/core/designsystem/component/UiStateBox.kt) | Loading/Empty/Error/Offline/Success |
| `PulsingRing` | [AnimationFoundation.kt](../../android/app/src/main/java/com/mesh/emergency/core/designsystem/component/AnimationFoundation.kt) | Emergency pulse indicator |
| `ScanningRipple` | [AnimationFoundation.kt](../../android/app/src/main/java/com/mesh/emergency/core/designsystem/component/AnimationFoundation.kt) | BLE/LoRa scanning animation |
| `ShimmerPlaceholder` | [AnimationFoundation.kt](../../android/app/src/main/java/com/mesh/emergency/core/designsystem/component/AnimationFoundation.kt) | Loading skeleton |

---

## Accessibility Rules

- **Minimum touch target**: 48×48dp (`Spacing.sosTouchTarget = 72dp` for SOS)
- **Text contrast**: All text/background combos target WCAG AA (4.5:1 ratio)
- **Dynamic font sizes**: All text uses `sp` units — scales with system accessibility settings
- **Content descriptions**: Every interactive element must have a unique `contentDescription`
- **No motion-only information**: All animated states (pulsing SOS) also show a text label

---

## Spacing System (4dp grid)

| Token | Value | Typical use |
|---|---|---|
| `xs` | 4dp | Icon gaps |
| `sm` | 8dp | List item spacing |
| `md` | 12dp | List item padding |
| `lg` | 16dp | Card padding / screen edges |
| `xxl` | 24dp | Section gaps |
| `xxxl` | 32dp | Large section gaps |
| `sosTouchTarget` | 72dp | SOS button minimum |
