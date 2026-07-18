# Android Architecture & UI Guide — Phase A26 + A27

## Architecture Overview

```
 ┌──────────────────────────────────────────────────────┐
 │                 Presentation Layer                   │
 │  Composable Screens  ←→  ViewModels (MVI)            │
 │  AppShell  ·  NavGraph  ·  Adaptive Layout           │
 └──────────────────────────────┬───────────────────────┘
                                ↓
 ┌──────────────────────────────────────────────────────┐
 │                   Domain Layer                       │
 │  AppState  ·  AppStateRepository (interface)         │
 │  UseCases (Phases A28+)                              │
 └──────────────────────────────┬───────────────────────┘
                                ↓
 ┌──────────────────────────────────────────────────────┐
 │                    Data Layer                        │
 │  AppStateRepositoryImpl  ·  Room DAOs                │
 │  DataStore Preferences (Phase A28)                   │
 └──────────────────────────────────────────────────────┘
```

---

## Project Structure

```
app/
 ├── app/
 │    ├── MainActivity.kt          ← Entry point, Hilt, Edge-to-Edge
 │    └── MeshApplication.kt       ← HiltAndroidApp, Timber init
 ├── core/
 │    ├── data/
 │    │    └── AppStateRepositoryImpl.kt
 │    ├── designsystem/            ← (Phase A25)
 │    ├── domain/
 │    │    └── AppState.kt         ← Global state model + repo interface
 │    ├── navigation/
 │    │    ├── AppNavGraph.kt      ← NavHost + all composable routes
 │    │    ├── NavRoutes.kt        ← Route string constants
 │    │    └── NavigationDestination.kt
 │    └── presentation/
 │         ├── AppShell.kt         ← Root scaffold + adaptive nav
 │         └── base/               ← BaseViewModel / MVI contracts
 ├── di/
 │    ├── AppStateModule.kt        ← Hilt binding: AppStateRepository
 │    └── (20 other modules)
 └── feature/
      ├── dashboard/
      │    ├── SplashViewModel.kt / SplashScreen.kt
      │    ├── HomeViewModel.kt    / HomeScreen.kt
      │    └── NetworkScreen.kt
      ├── contacts/
      │    ├── DeviceViewModel.kt  / DeviceScreen.kt
      ├── chat/
      │    └── CommunicationScreen.kt
      └── settings/
           ├── SettingsViewModel.kt / SettingsScreen.kt
```

---

## MVI Pattern

All screens use `BaseViewModel<State, Event, Effect>`:

```kotlin
// 1. Declare State
data class HomeUiState(val isOnline: Boolean = false) : BaseUiState

// 2. Declare Events
sealed interface HomeUiEvent : BaseUiEvent {
    data object RefreshStatus : HomeUiEvent
}

// 3. Declare Effects (one-shot)
sealed interface HomeUiEffect : BaseUiEffect {
    data class ShowToast(val message: String) : HomeUiEffect
}

// 4. ViewModel
@HiltViewModel
class HomeViewModel @Inject constructor(...) :
    BaseViewModel<HomeUiState, HomeUiEvent, HomeUiEffect>(HomeUiState()) {

    override fun onEvent(event: HomeUiEvent) { /* handle */ }
}

// 5. Screen
@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.effect.collect { /* handle effects */ }
    }
}
```

---

## Navigation Flow

```
Splash (1.2s init)
    ↓ NavigateToHome (backstack cleared)
Home Dashboard
    ├── [Bottom Nav] → Chats (CommunicationScreen)
    ├── [Bottom Nav] → Contacts (DeviceScreen)
    ├── [Bottom Nav] → Network (NetworkScreen)
    ├── [Bottom Nav] → Settings (SettingsScreen)
    └── [SOS Button] → Emergency (Placeholder → Phase A29)
```

---

## AppState — Global State

All screens observe `AppStateRepository.appState: StateFlow<AppState>`:

| Field | Type | Default | Updated by |
|---|---|---|---|
| `themeMode` | `ThemeMode` | `SYSTEM` | SettingsViewModel |
| `languageCode` | `String` | `"en"` | SettingsViewModel |
| `isOnline` | `Boolean` | `false` | Phase A28 (BLE/LoRa) |
| `batteryLevel` | `Float` | `1.0` | Phase A28 (PowerManager) |
| `isCharging` | `Boolean` | `false` | Phase A28 |
| `activeSos` | `Boolean` | `false` | HomeViewModel |
| `connectedNodeCount` | `Int` | `0` | Phase A28 |
| `activeTransport` | `String` | `"NONE"` | Phase A28 |

---

## Responsive Layout

| Breakpoint | Layout |
|---|---|
| `WindowSize.COMPACT` (phones) | Bottom Navigation Bar |
| `WindowSize.MEDIUM` (large phones/foldables) | Navigation Rail |
| `WindowSize.EXPANDED` (tablets) | Navigation Rail + Drawer |

Implemented in [`AppShell.kt`](../../android/app/src/main/java/com/mesh/emergency/core/presentation/AppShell.kt)

---

## Screens Implemented

| Screen | Route | ViewModel | Status |
|---|---|---|---|
| Splash | `splash` | `SplashViewModel` | ✅ Phase A27 |
| Home Dashboard | `home` | `HomeViewModel` | ✅ Phase A27 |
| Devices | `contacts` | `DeviceViewModel` | ✅ Phase A27 |
| Network | `network-dashboard` | — (stateless) | ✅ Phase A27 |
| Communication | `chat-list` | — (stateless) | ✅ Phase A27 |
| Settings | `settings` | `SettingsViewModel` | ✅ Phase A27 |
| Emergency | `emergency` | — | ⏳ Phase A29 |
| Chat Detail | `chat/{id}` | — | ⏳ Phase A28 |
| QR Pair | `qr-pair` | — | ⏳ Phase A28 |
| Map | `map` | — | ⏳ Phase A30 |
