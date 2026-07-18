# State Management

The Android application implements state management using **Jetpack Compose** coupled with **MVVM Architecture** patterns. Asynchrony and streaming database updates are handled via **Kotlin Coroutines** and **Flows**.

---

## 1. Unidirectional Data Flow (UDF)

The application enforces a Unidirectional Data Flow architecture:
1. **Events** flow from UI components (Compose screen) up to the ViewModels (e.g., typing text, clicking "Send").
2. **State Updates** flow from the business logic layer down to the UI. ViewModels observe data repositories via Flows, encapsulate data within UI State classes, and expose it via state holders.
3. The UI recomposes automatically whenever the observed state changes.

```
┌────────────────────────────────────────┐
│               UI Screen                │
│    (Observe uiState via Compose)       │
└──────────────────┬─────────────────────┘
                   │ User Event (Actions)
                   ▼
┌────────────────────────────────────────┐
│               ViewModel                │
│       (Transforms flows into State)    │
└──────────────────┬─────────────────────┘
                   │ Use Case execution
                   ▼
┌────────────────────────────────────────┐
│              Repository                │
│      (Local DB queries & BLE streams)  │
└────────────────────────────────────────┘
```

---

## 2. Exposing State with StateFlow

ViewModels utilize `MutableStateFlow` internally and expose read-only `StateFlow` to Compose UI. Screen states are structured as immutable Kotlin data classes representing the entirety of the UI state.

```kotlin
// UI State Wrapper Example
data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val isPaired: Boolean = false,
    val isLoading: Boolean = false,
    val connectionState: BleConnectionState = BleConnectionState.DISCONNECTED
)
```

In the corresponding ViewModel, this is initialized and updated securely:

```kotlin
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val getMessagesUseCase: GetMessagesUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    private val getBleConnectionUseCase: GetBleConnectionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Combine DB changes and Connection state dynamically
            combine(
                getMessagesUseCase(),
                getBleConnectionUseCase()
            ) { messages, connection ->
                ChatUiState(
                    messages = messages,
                    connectionState = connection,
                    isLoading = false
                )
            }.collect { updatedState ->
                _uiState.value = updatedState
            }
        }
    }

    fun handleAction(action: ChatAction) {
        when (action) {
            is ChatAction.SendMessage -> {
                viewModelScope.launch {
                    sendMessageUseCase(action.text, action.recipientId)
                }
            }
        }
    }
}
```

---

## 3. Offline-First State Synchronization

Because this system is offline-first, state synchronization operates primarily with the local SQLite (Room) Database as the **single source of truth**.

1. **Local Write First**: When a user submits a message, the message is written immediately to Room with a status of `DeliveryStatus.QUEUED`. The UI updates reactively from Room streams.
2. **Transport Attempt**: The `CommunicationManager` captures the event, evaluates the connection state, and pushes the payload to the BLE transport buffers.
3. **Status Update**: Upon confirmation of BLE write, the state updates to `SENT`. If an ACK packet is routed back from the mesh network, the status in Room is updated to `DELIVERED`. The UI automatically reflects this change through the reactive DB flow.

---

## 4. Key ViewModels and Flow Maps

### A. Home Dashboard State
* Exposes lists of open conversations sorted by `last_message_at`.
* Monitors global BLE state to alert the user of hardware disconnection.

### B. Map Screen State
* Streams location lists from `location_share` table.
* Handles location updates via the device's Fused Location provider, pushing new coordinates into the local store.

### C. Emergency/SOS State
* Manages the lifecycle of an active emergency broadcast.
* Exposes countdown intervals for periodic broadcast retries.
