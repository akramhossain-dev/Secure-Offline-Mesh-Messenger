# Architecture Overview — Phase A1

## Pattern: Clean Architecture + MVVM

The Offline Emergency Mesh Communication System follows **Clean Architecture** with **MVVM** presentation pattern. This enforces strict separation of concerns, making the codebase:

- **Testable** — domain logic has no Android dependencies
- **Maintainable** — each layer has a single responsibility
- **Scalable** — new features are added without modifying existing layers

---

## Layer Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                     Presentation Layer                          │
│  ┌───────────────────┐       ┌───────────────────┐             │
│  │  Compose Screens  │ ◄───► │    ViewModels     │             │
│  │  (Stateless UI)   │       │  (StateFlow, UDF) │             │
│  └───────────────────┘       └─────────┬─────────┘             │
└─────────────────────────────────────────│───────────────────────┘
                                          │ invokes
┌─────────────────────────────────────────▼───────────────────────┐
│                       Domain Layer                              │
│  ┌────────────────┐  ┌────────────────┐  ┌──────────────────┐  │
│  │   Use Cases    │  │ Domain Models  │  │ Repository       │  │
│  │ (1 per action) │  │ (pure Kotlin)  │  │ Interfaces       │  │
│  └────────────────┘  └────────────────┘  └─────────┬────────┘  │
└─────────────────────────────────────────────────────│───────────┘
                                                      │ implemented by
┌─────────────────────────────────────────────────────▼───────────┐
│                        Data Layer                               │
│  ┌────────────────┐  ┌────────────────┐  ┌──────────────────┐  │
│  │  Repository    │  │  Room Database │  │ Communication    │  │
│  │  Impls         │  │  (DAOs, DAOs)  │  │ Manager (BLE,    │  │
│  └────────────────┘  └────────────────┘  │ LoRa, S&F)       │  │
│                                           └──────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

---

## Package Structure

```
com.mesh.emergency/
│
├── app/                          # Application + Activity
│   ├── MeshApplication.kt        # @HiltAndroidApp, Timber init
│   └── MainActivity.kt           # Compose host, edge-to-edge
│
├── core/
│   ├── common/
│   │   ├── config/               # AppConfig, BuildInfo, FeatureFlags
│   │   ├── constants/            # AppConstants (BLE UUIDs, keys, etc.)
│   │   ├── extensions/           # Context, String, Flow extensions
│   │   ├── localization/         # LocaleManager, SupportedLanguage
│   │   ├── logging/              # AppLogger (Timber wrapper)
│   │   └── result/               # Result<T> sealed class
│   │
│   ├── designsystem/
│   │   └── theme/                # Color, Theme, Typography, Shape, Spacing
│   │
│   ├── navigation/               # NavRoutes (route constants)
│   └── utils/                    # DateUtils, PermissionHelper
│
├── di/                           # Hilt modules
│   └── AppModule.kt              # DataStore provision
│
├── feature/                      # Feature modules (Phase A2+)
│   ├── chat/
│   ├── contacts/
│   ├── emergency/
│   ├── map/
│   ├── dashboard/
│   ├── settings/
│   └── profile/
│
├── domain/                       # Domain layer (Phase A2+)
│   ├── model/                    # Pure Kotlin domain models
│   ├── usecase/                  # Use case interfaces
│   └── repository/               # Repository interfaces
│
├── data/                         # Data layer (Phase A2+)
│   ├── local/                    # Room database, DAOs
│   ├── communication/            # BLE + LoRa + Store & Forward
│   ├── crypto/                   # AES-256-GCM encryption
│   └── repository/               # Repository implementations
│
└── hardware/                     # Hardware abstraction (Phase A2+)
    ├── bluetooth/                # BLE hardware interface
    └── lora/                     # LoRa hardware interface
```

---

## Dependency Rules

```
Presentation → Domain ← Data
             ↑
        (No reverse dependencies)
```

| Layer | Can Import | Cannot Import |
|---|---|---|
| Presentation | Domain only | Data, Hardware |
| Domain | Nothing (pure Kotlin) | Presentation, Data, Android |
| Data | Domain (interfaces) | Presentation |
| Hardware | Nothing | All layers |

---

## Key Design Decisions

### 1. Unidirectional Data Flow (UDF)
All state flows from ViewModel → Compose. User events flow Compose → ViewModel → UseCase.

```
User action → Composable → ViewModel.onEvent() → UseCase → Repository
                                ↑
Result → StateFlow<UiState> → Composable rerenders
```

### 2. StateFlow for UI state
Each ViewModel exposes a single `StateFlow<UiState>` representing the complete screen state:

```kotlin
data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)
```

### 3. Result wrapper
All repository and use case functions return `Result<T>`:
- `Result.Loading` — operation in progress
- `Result.Success(data)` — operation succeeded
- `Result.Error(exception)` — operation failed

### 4. Hilt for dependency injection
All `@Singleton` and `@HiltViewModel` annotated classes are injected automatically. No manual factory construction.

### 5. Feature flags
Every feature is gated behind a `FeatureFlags` constant. In Phase A1, all flags are `false`. This allows safe deployment of incomplete features.

---

## Phase A1 Status

Phase A1 implements the **foundation only**:

| Component | Status |
|---|---|
| Gradle + Version Catalog | ✅ Complete |
| Package structure | ✅ Stubs created |
| Material Design 3 theme | ✅ Complete |
| Localization infrastructure | ✅ Complete |
| Logging (Timber) | ✅ Complete |
| Result wrapper | ✅ Complete |
| Extension functions | ✅ Complete |
| DI foundation (Hilt + DataStore) | ✅ Complete |
| Code quality tooling | ✅ Complete |
| Git foundation | ✅ Complete |
| Feature modules | 🔲 Stubs only (Phase A2+) |
| Navigation graph | 🔲 Phase A2 |
| Room Database | 🔲 Phase A2 |
| BLE transport | 🔲 Phase A2 |
| LoRa transport | 🔲 Phase A3 |

---

## Related Documentation

- [App Overview](../app/app-overview.md) — technical stack
- [App Architecture](../app/app-architecture.md) — detailed MVVM design
- [Theme Overview](theme-overview.md) — design system
- [Localization Overview](localization-overview.md) — i18n infrastructure
- [Setup Guide](setup-guide.md) — developer environment setup
