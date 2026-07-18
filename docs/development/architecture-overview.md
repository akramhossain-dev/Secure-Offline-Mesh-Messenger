# Architecture Overview — Phase A2

## Pattern: Clean Architecture + MVVM + MVI

The Offline Emergency Mesh Communication System follows **Clean Architecture** with an **MVI-inspired MVVM** (Model-View-Intent / Model-View-ViewModel) presentation pattern. This enforces strict separation of concerns, making the codebase testable, maintainable, and robust against offline sync environments.

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
                                          │ invokes UseCases
┌─────────────────────────────────────────▼───────────────────────┐
│                       Domain Layer                              │
│  ┌────────────────┐  ┌────────────────┐  ┌──────────────────┐  │
│  │   Use Cases    │  │ Domain Models  │  │ Repository       │  │
│  │ (Suspend/Flow) │  │  (BaseModel)   │  │ Interfaces       │  │
│  └────────────────┘  └────────────────┘  └─────────┬────────┘  │
└─────────────────────────────────────────────────────│───────────┘
                                                      │ implemented by
┌─────────────────────────────────────────────────────▼───────────┐
│                        Data Layer                               │
│  ┌────────────────┐  ┌────────────────┐  ┌──────────────────┐  │
│  │  Repository    │  │  Room Database │  │ Communication    │  │
│  │  Impls         │  │ (BaseEntity DB)│  │ Managers (BLE,   │  │
│  └────────────────┘  └────────────────┘  │ LoRa stubs)      │  │
└──────────────────────────────────────────┴──────────────────────┘
```

---

## Package Structure

```
com.mesh.emergency/
│
├── app/                          # Application + Activity
│   ├── MeshApplication.kt        # @HiltAndroidApp, Timber/Logger init
│   └── MainActivity.kt           # Compose host, edge-to-edge
│
├── core/
│   ├── common/
│   │   ├── config/               # AppConfiguration, Feature, AppConfig, BuildInfo
│   │   ├── constants/            # AppConstants (BLE UUIDs, keys, etc.)
│   │   ├── extensions/           # Context, String, Flow extensions
│   │   ├── localization/         # LocaleManager, SupportedLanguage
│   │   ├── logging/              # Logger, LoggerImpl, AppLogger (wrapper)
│   │   └── result/               # Result<T> sealed class
│   │
│   ├── communication/            # TransportManager, TransportManagerStub, ConnectionState
│   ├── designsystem/
│   │   └── theme/                # Color, Theme, Typography, Shape, Spacing
│   │
│   ├── error/                    # AppException, Failure, ErrorMapper
│   ├── model/                    # BaseModel (domain), BaseEntity (data)
│   ├── navigation/               # NavRoutes, NavigationDestination, AppNavigator
│   ├── presentation/
│   │   └── base/                 # BaseUiState, BaseUiEvent, BaseUiEffect, BaseViewModel, BaseScreen
│   ├── storage/                  # StorageManager, StorageManagerImpl
│   └── utils/                    # LocationProvider, LocationProviderStub, PermissionManager, PermissionManagerImpl
│
├── di/                           # Hilt modules
│   ├── AppModule.kt              # DataStore + StorageManager provision
│   ├── CoreModule.kt             # Logger, Transport, Config bindings
│   ├── UtilityModule.kt          # PermissionManager, LocationProvider bindings
│   ├── DispatcherModule.kt       # IO, Default, Main Dispatchers
│   └── RepositoryModule.kt       # Repositories contracts bindings
│
├── feature/                      # Feature modules (stubs)
├── domain/                       # Domain layer contracts
│   └── repository/               # UserRepository, MessageRepository, DeviceRepository, etc.
│
└── data/                         # Data layer
    └── repository/               # UserRepositoryImpl, MessageRepositoryImpl, etc.
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
| **Presentation** | `Domain`, `Core` | `Data`, `Hardware` |
| **Domain** | `Core` (only models/contracts) | `Presentation`, `Data`, Android SDK |
| **Data** | `Domain` (interfaces), `Core` | `Presentation` |
| **Core** | Minimal external deps (Base types only) | `Presentation`, `Domain`, `Data` |

---

## MVVM / MVI Implementation Rules

### 1. Unidirectional Data Flow (UDF)
Compose screens observe a single read-only state and pass user actions to ViewModels as events.

- **State (`BaseUiState`)**: Represents the absolute state of the UI at any point. Must be immutable.
- **Event (`BaseUiEvent`)**: User intentions triggered by widgets (e.g. typing, clicking buttons).
- **Effect (`BaseUiEffect`)**: Fire-and-forget transient commands handled on the UI thread (e.g. Navigating, showing Snackbar).

```
Compose Layout ──[Event]──► BaseViewModel.onEvent() ──► Business Logic
      ▲                                                    │
      │                                                Update State
      └──────────[StateFlow<BaseUiState>]──────────────────┘
```

### 2. BaseViewModel Contract
Every ViewModel must extend `BaseViewModel`:
```kotlin
class ChatViewModel @Inject constructor(
    private val sendMessageUseCase: SendMessageUseCase
) : BaseViewModel<ChatUiState, ChatUiEvent, ChatUiEffect>(ChatUiState()) {

    override fun onEvent(event: ChatUiEvent) {
        when (event) {
            is ChatUiEvent.SendMessage -> performSend(event.text)
        }
    }
}
```

---

## Use Case Architectural Mechanics

Use Cases execute business logic off the Main thread and return standard `Result` wrappers:

- **`SuspendUseCase`**: Use for single one-shot transactional operations (e.g. setting profile, sending commands).
- **`FlowUseCase`**: Use for continuous data stream queries (e.g. database change observers, location updates).

---

## Error Handling Standards

All exceptions should be parsed into user-friendly failures:
1. Lower layers raise an `AppException` (e.g. `AppException.Database`, `AppException.Bluetooth`).
2. UseCases/Repositories catch standard exceptions and map them.
3. `ErrorMapper` maps `AppException` to a domain-level `Failure` class (e.g. `Failure.Database`).
4. Presentation displays appropriate error strings using the `Failure` details.

---

## Navigation Architecture

Navigation is coordinate-managed:
- **`NavigationDestination`** specifies logical paths (e.g. `ChatDetail.createRoute(id)`).
- **`AppNavigator`** coordinates the NavActions list. ViewModels inject `AppNavigator` to trigger route jumps without direct references to Jetpack Navigation components.

---

## Phase A2 Status

Phase A2 implements the **complete core application architecture**:

| Component | Status | Description |
|---|---|---|
| **MVVM Foundation** | ✅ Complete | BaseUiState, BaseUiEvent, BaseUiEffect, BaseViewModel, BaseScreen |
| **DI Abstractions** | ✅ Complete | DispatcherModule, CoreModule, UtilityModule, RepositoryModule |
| **Repository Contracts**| ✅ Complete | UserRepository, MessageRepository, DeviceRepository, SettingsRepository, etc. |
| **Use Case Core** | ✅ Complete | SuspendUseCase, FlowUseCase, BaseUseCase |
| **Error Handling** | ✅ Complete | AppException, Failure, ErrorMapper |
| **Navigation Core** | ✅ Complete | NavigationDestination, AppNavigator, NavigationAction |
| **Service Contracts** | ✅ Complete | Logger, PermissionManager, LocationProvider, TransportManager, StorageManager |
| **Config Core** | ✅ Complete | AppConfiguration, AppConfigurationImpl, Feature enum |

---

## Related Documentation

- [App Overview](../app/app-overview.md) — technical stack
- [App Architecture](../app/app-architecture.md) — detailed MVVM design
- [Theme Overview](theme-overview.md) — design system
- [Localization Overview](localization-overview.md) — i18n infrastructure
- [Setup Guide](setup-guide.md) — developer environment setup
