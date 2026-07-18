# Theme System Overview

## Design Philosophy

The Offline Emergency Mesh Communication System uses **Material Design 3** as its design foundation, customized with an **emergency communications aesthetic**:

- **Deep Indigo primary** — authority, trust, tactical communications
- **Electric Teal secondary** — signal strength, connectivity, data flow
- **Amber tertiary** — alerts, warnings, power telemetry
- **Emergency Red error** — SOS, critical failures, danger states

---

## Color System

### Tonal Palettes

The theme uses five tonal palettes, each with 11 shades:

| Palette | Role | Hex (50%) |
|---|---|---|
| **Indigo** | Primary | `#4F4BD4` |
| **Teal** | Secondary | `#007178` |
| **Amber** | Tertiary / Alerts | `#FFB82A` |
| **Red** | Error / SOS | `#BA1A1A` |
| **Neutral** | Surfaces, backgrounds | — |

### Light vs. Dark Scheme

Material 3 color tokens are mapped separately for light and dark:

```kotlin
// Light scheme
md_theme_light_primary = Indigo50 (#4F4BD4)
md_theme_light_background = Neutral99 (#FFFBFF)

// Dark scheme
md_theme_dark_primary = Indigo80 (#C4C3EE)
md_theme_dark_background = Neutral10 (#1B1B1F)
```

All token mappings are in [`Color.kt`](../../android/app/src/main/java/com/mesh/emergency/core/designsystem/theme/Color.kt).

---

## Theme Modes

Three modes are supported via `ThemeMode` enum:

| Mode | Behavior |
|---|---|
| `SYSTEM` | Follows Android system dark/light setting (default) |
| `LIGHT` | Always light theme |
| `DARK` | Always dark theme |

The mode is persisted in DataStore (`PREF_KEY_THEME_MODE`) and applied at startup.

### Dynamic Color (Android 12+)

When `dynamicColor = true`, the theme derives colors from the user's wallpaper using Android's Material You system. This overrides the Indigo/Teal palette but preserves the emergency semantic colors (`ColorSosActive`, `ColorBleConnected`, etc.).

Dynamic color is opt-in and configurable in Settings.

---

## Typography

The app uses **Inter** (by Google Fonts) — chosen for its high legibility at small sizes, essential for emergency dashboards.

Full Material 3 type scale implemented (15 roles):

| Role | Size | Weight | Use |
|---|---|---|---|
| `displayLarge` | 57sp | Normal | Hero screens |
| `headlineLarge` | 32sp | SemiBold | Screen titles |
| `titleLarge` | 22sp | SemiBold | App bar titles |
| `bodyLarge` | 16sp | Normal | Message content |
| `bodyMedium` | 14sp | Normal | Secondary content |
| `labelLarge` | 14sp | Medium | Button labels |
| `labelSmall` | 11sp | Medium | Captions, timestamps |

---

## Shape System

Corner radii follow a 4dp-based scale:

| Token | Value | Used For |
|---|---|---|
| `extraSmall` | 4dp | Chips, badges, input corners |
| `small` | 8dp | Buttons, small cards |
| `medium` | 12dp | Standard cards, dialogs |
| `large` | 16dp | Navigation drawers |
| `extraLarge` | 28dp | Bottom sheets, modal surfaces |

---

## Spacing System

The `Spacing` data class provides a 4dp-grid token system accessed via `LocalSpacing.current`:

```kotlin
@Composable
fun MyComposable() {
    val spacing = LocalSpacing.current
    Column(
        modifier = Modifier.padding(
            horizontal = spacing.screenHorizontal,  // 16dp
            vertical = spacing.screenVertical,      // 16dp
        )
    ) { ... }
}
```

Key semantic tokens:

| Token | Value | Purpose |
|---|---|---|
| `screenHorizontal` | 16dp | Screen edge padding |
| `cardPadding` | 16dp | Card internal padding |
| `listItemSpacing` | 8dp | Gap between list items |
| `sosTouchTarget` | 72dp | Minimum SOS button touch target |
| `iconSizeMd` | 24dp | Standard icon size |

---

## Theme Switching

Theme switching infrastructure (Phase A1):

1. `ThemeMode` enum — defines the three modes
2. `AppConstants.PREF_KEY_THEME_MODE` — DataStore key
3. `MeshTheme(themeMode = ...)` — applies the selected mode

Phase A2 will add:
- `ThemeViewModel` — reads/writes preference via DataStore
- Settings screen UI — theme picker with System/Light/Dark options
- Dynamic color toggle

---

## Semantic Colors

Beyond the M3 scheme, the design system defines app-specific semantic colors:

| Color | Hex | Usage |
|---|---|---|
| `ColorSosActive` | `#FF1744` | SOS broadcast active indicator |
| `ColorBleConnected` | Teal50 | BLE connection status |
| `ColorLoraActive` | Amber70 | LoRa mesh active indicator |
| `ColorDelivered` | Teal60 | Message delivered status |
| `ColorPending` | Neutral60 | Message queued status |
| `ColorFailed` | Red50 | Message delivery failed |

---

## Files Reference

| File | Purpose |
|---|---|
| [`Color.kt`](../../android/app/src/main/java/com/mesh/emergency/core/designsystem/theme/Color.kt) | All color values and M3 scheme tokens |
| [`Theme.kt`](../../android/app/src/main/java/com/mesh/emergency/core/designsystem/theme/Theme.kt) | `MeshTheme` composable |
| [`Typography.kt`](../../android/app/src/main/java/com/mesh/emergency/core/designsystem/theme/Typography.kt) | M3 type scale |
| [`Shape.kt`](../../android/app/src/main/java/com/mesh/emergency/core/designsystem/theme/Shape.kt) | Shape system |
| [`Spacing.kt`](../../android/app/src/main/java/com/mesh/emergency/core/designsystem/theme/Spacing.kt) | Spacing tokens |
| [`ThemeConfig.kt`](../../android/app/src/main/java/com/mesh/emergency/core/designsystem/theme/ThemeConfig.kt) | `ThemeMode` enum |
