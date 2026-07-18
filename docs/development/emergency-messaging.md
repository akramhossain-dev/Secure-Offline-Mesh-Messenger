# Emergency & Messaging System — Phase A28 + A29

## Emergency UI Flow

```
User long-presses SOS
        ↓
  SosState.CONFIRMING
        ↓
  AlertDialog (confirm / cancel)
        ↓   ↘
 CONFIRM      CANCEL → READY
        ↓
  SosState.ACTIVE
  EmergencyEvent created → Room DB
        ↓
  Queued for mesh delivery (Phase A30)
        ↓
  User taps "Acknowledge" → ACKNOWLEDGED
        ↓
  User taps "Resolve"     → RESOLVED (isResolved=true)
```

### SOS State Machine

| State | Trigger | Visual |
|---|---|---|
| `READY` | Initial | SOS button visible |
| `CONFIRMING` | Button pressed | AlertDialog shown |
| `ACTIVE` | Dialog confirmed | PulsingRing + emergency banner |
| `ACKNOWLEDGED` | Operator ack | ✓ green icon |
| `RESOLVED` | Marked resolved | ✓✓ green, moved to history |

---

## Emergency Feature Architecture

```
feature/emergency/
 ├── domain/
 │    └── EmergencyDomain.kt     ← EmergencyEvent, SosState, EmergencyRepository, mappers
 ├── data/
 │    └── EmergencyRepositoryImpl.kt  ← Room DAO backed impl
 └── presentation/
      ├── EmergencyViewModel.kt  ← MVI: SOS workflow + DB sync
      └── EmergencyScreen.kt     ← Full UI
```

### Room Integration

- **Entity**: `EmergencyEventEntity` (table: `emergency_events`)
- **DAO**: `EmergencyEventDao` — `getEmergencyEvents()` Flow, `insertEmergencyEvent()`, `getEmergencyEventById()`
- **Status transitions**: `CREATED → BROADCASTING → RECEIVED → ACKNOWLEDGED → RESOLVED`

---

## Messaging Architecture

```
feature/message/
 ├── domain/
 │    └── MessageDomain.kt       ← Message, ConversationSummary, MessageRepository, mappers
 ├── data/
 │    └── MessageRepositoryImpl.kt  ← MessageDao + ConversationDao backed impl
 └── presentation/
      ├── MessageViewModels.kt   ← MessageListViewModel + ChatViewModel (MVI)
      ├── MessageListScreen.kt   ← Conversation list
      └── ChatScreen.kt          ← Full offline chat UI
```

### Room Integration

- **Messages**: `MessageEntity` (table: `messages`) — stores content, delivery status, priority, retryCount
- **Conversations**: `ConversationEntity` (table: `conversations`) — aggregates by convId
- **Queries**: `MessageDao.getMessagesForConversation(convId)` → `Flow<List<MessageEntity>>`

---

## Offline Messaging Experience

| Scenario | Behavior |
|---|---|
| No nodes nearby | Message saved as `QUEUED`, pending banner shown in chat |
| Node becomes reachable | Queue drains automatically (Phase A30) |
| Delivery confirmed | Status updates `QUEUED → SENT → DELIVERED` |
| Failed after retries | Status `FAILED`, retryCount shown |
| Message expired (TTL) | Status `EXPIRED` |

### Delivery Status Indicators

| Status | Symbol | Color |
|---|---|---|
| `PENDING` | ⏳ | Warning amber |
| `QUEUED` | 📦 | Warning amber |
| `SENDING` | ↑ | Info blue |
| `SENT` | ✓ | Info blue |
| `DELIVERED` | ✓✓ | Success green |
| `FAILED` | ✗ | Emergency red |
| `EXPIRED` | ⌛ | Emergency red |

---

## Priority Visualization

| Priority | Badge | Color |
|---|---|---|
| `CRITICAL` | CRITICAL chip | Emergency red |
| `HIGH` | HIGH chip | Warning amber |
| `MEDIUM` / `NORMAL` | NORMAL chip | Info blue |
| `LOW` | LOW chip | Disabled grey |

Priority is shown on:
- Message bubbles (! indicator)
- Emergency event cards (PriorityBadge)
- Pending message rows (PriorityBadge)

---

## Navigation Routes Added (A28/A29)

| Route | Screen |
|---|---|
| `emergency` | EmergencyScreen (existing destination) |
| `emergency-dashboard` | EmergencyScreen (alias via HomeScreen SOS) |
| `sos-active` | EmergencyScreen (SOS deep-link) |
| `chat-list` | MessageListScreen |
| `chat-screen/{convId}/{label}` | ChatScreen |

---

## Test Coverage

- [`EmergencyMessagingTest.kt`](../../android/app/src/test/java/com/mesh/emergency/EmergencyMessagingTest.kt): 22 tests covering SOS workflow, event partitioning, delivery status labels, isSelf logic, pending count, and UI state defaults
