# Testing

---

## Test Categories Overview

| Category | Scope | Tooling |
|---|---|---|
| Unit Tests | Domain logic, use cases, serialization | JUnit 5, Kotlin Test |
| Integration Tests | Room Database, repository layer | JUnit 4, Room in-memory |
| UI Tests | Compose screens, user flows | Espresso, Compose Test |
| Firmware Unit Tests | Packet serialization, routing logic | Unity (ESP32 native test) |
| Hardware Tests | LoRa range, signal quality, power | Physical measurement |
| Field Tests | Multi-node mesh, emergency scenarios | Live deployment |

---

## Software Testing — Android

### Unit Tests

Location: `app/src/test/`

#### Use Case Tests

```kotlin
class SendMessageUseCaseTest {

    private val mockRepository = mockk<MessageRepository>()
    private val mockCrypto = mockk<CryptoEngine>()
    private val useCase = SendMessageUseCase(mockRepository, mockCrypto)

    @Test
    fun `send message encrypts payload before storing`() = runTest {
        val plaintext = "Test message"
        val contactKey = generateTestPublicKey()
        every { mockCrypto.encrypt(plaintext, contactKey) } returns "encrypted"

        useCase(plaintext, recipientId = "contact-uuid", recipientKey = contactKey)

        verify { mockRepository.sendMessage(match { it.content == "encrypted" }) }
    }
}
```

#### Packet Serialization Tests

```kotlin
class PacketSerializerTest {

    @Test
    fun `serialize and deserialize TEXT packet preserves all fields`() {
        val packet = Packet(
            id = "test-uuid",
            type = PacketType.TEXT,
            sender = "sender-uuid",
            receiver = "receiver-uuid",
            priority = 2,
            payload = "base64payload",
            timestamp = 1752748800000L,
            ttl = 5,
            signature = "test-signature"
        )
        val json = PacketSerializer.serialize(packet)
        val deserialized = PacketSerializer.deserialize(json)
        assertEquals(packet, deserialized)
    }

    @Test
    fun `serialize SOS packet includes all required fields`() {
        val packet = buildSosPacket(lat = 23.8103, lon = 90.4125, message = "Help")
        val json = PacketSerializer.serialize(packet)
        assertContains(json, "\"type\":\"SOS\"")
        assertContains(json, "\"lat\":23.8103")
    }
}
```

**Target coverage: ≥ 80% for domain and data layers.**

---

### Integration Tests (Room Database)

Location: `app/src/androidTest/`

```kotlin
@RunWith(AndroidJUnit4::class)
class MessageDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: MessageDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.messageDao()
    }

    @After
    fun teardown() { db.close() }

    @Test
    fun insertMessage_retrievedInConversation() = runTest {
        val message = testMessageEntity(conversationId = "conv-1")
        dao.insert(message)
        val messages = dao.getMessagesByConversation("conv-1").first()
        assertEquals(1, messages.size)
        assertEquals(message.id, messages[0].id)
    }

    @Test
    fun updateDeliveryStatus_reflectedInQuery() = runTest {
        val msg = testMessageEntity(conversationId = "conv-1")
        dao.insert(msg)
        dao.updateDeliveryStatus(msg.id, DeliveryStatus.DELIVERED.ordinal)
        val updated = dao.getMessagesByConversation("conv-1").first()[0]
        assertEquals(DeliveryStatus.DELIVERED.ordinal, updated.deliveryStatus)
    }
}
```

**Test Scenarios:**

| Scenario | Expected Result |
|---|---|
| Insert message, query by conversation | Message returned in correct conversation |
| Insert duplicate message ID | Ignored (OnConflictStrategy.IGNORE) |
| Update delivery status | Status change reflected in subsequent query |
| Unread count query | Returns correct count of is_read=0 messages |
| Contact upsert | Second insert with same node_id updates existing record |
| Location share expiry | Expired records identifiable by expires_at < now |

---

### UI Tests (Compose)

Location: `app/src/androidTest/`

```kotlin
@HiltAndroidTest
class ChatScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun sendMessage_appearsInConversationList() {
        composeTestRule.onNodeWithTag("message_input").performTextInput("Hello World")
        composeTestRule.onNodeWithTag("send_button").performClick()
        composeTestRule.onNodeWithText("Hello World").assertIsDisplayed()
    }

    @Test
    fun sosButton_requiresConfirmationBeforeSending() {
        composeTestRule.onNodeWithTag("sos_button").performClick()
        composeTestRule.onNodeWithText("Confirm SOS").assertIsDisplayed()
        composeTestRule.onNodeWithTag("sos_cancel").performClick()
        composeTestRule.onNodeWithText("Confirm SOS").assertDoesNotExist()
    }
}
```

**UI Test Coverage Targets:**

| Screen | Scenarios Tested |
|---|---|
| Chat Screen | Send message, message appears, delivery status update |
| SOS Screen | Button shows confirm dialog, cancel works, activate works |
| Profile Screen | QR code displays, name can be edited |
| Contact Screen | Contact appears after QR scan simulation |
| Map Screen | Map renders, location button visible |
| Settings Screen | All toggles persist after navigation away and back |

---

## Software Testing — ESP32 Firmware

### Unit Tests (PlatformIO + Unity)

Location: `firmware/test/`

```cpp
// test/test_packet/test_packet_serializer.cpp

#include <unity.h>
#include "packet/PacketSerializer.h"

void test_serialize_text_packet() {
    Packet pkt;
    pkt.id = "test-uuid";
    pkt.type = PacketType::TEXT;
    pkt.sender = "sender-uuid";
    pkt.receiver = "receiver-uuid";
    pkt.priority = 2;
    pkt.payload = "aGVsbG8=";
    pkt.timestamp = 1752748800000ULL;
    pkt.ttl = 5;
    pkt.signature = "test-sig";

    String json = PacketSerializer::serialize(pkt);
    Packet result = PacketSerializer::deserialize(json);

    TEST_ASSERT_EQUAL_STRING("test-uuid", result.id.c_str());
    TEST_ASSERT_EQUAL(PacketType::TEXT, result.type);
    TEST_ASSERT_EQUAL(5, result.ttl);
}

void test_ttl_decrement() {
    Packet pkt;
    pkt.ttl = 3;
    MeshRouter::decrementTtl(pkt);
    TEST_ASSERT_EQUAL(2, pkt.ttl);
}

void test_seen_cache_deduplication() {
    SeenCache cache(128);
    String key = "sender-uuid|msg-uuid";
    TEST_ASSERT_FALSE(cache.contains(key));
    cache.add(key);
    TEST_ASSERT_TRUE(cache.contains(key));
}

int main() {
    UNITY_BEGIN();
    RUN_TEST(test_serialize_text_packet);
    RUN_TEST(test_ttl_decrement);
    RUN_TEST(test_seen_cache_deduplication);
    return UNITY_END();
}
```

Run with: `pio test -e native`

---

## Hardware Testing

### LoRa Range Test Protocol

**Setup:** Two nodes with identical firmware. One node transmits a PING packet every 2 seconds. The other node logs RSSI and SNR for each received packet.

**Test Distances:** 50 m, 100 m, 250 m, 500 m, 1 km, 2 km, 3 km

**Environment Variables:** Record for each test:
- Location type (open field / urban / wooded)
- Antenna height (ground level / 1.5 m / elevated)
- Antenna orientation (vertical / horizontal)
- Weather conditions

**Data Table Template:**

| Distance | Environment | RSSI (dBm) | SNR (dB) | Packet Loss (%) | Notes |
|---|---|---|---|---|---|
| 100 m | Open field | –72 | +12.5 | 0% | |
| 500 m | Open field | –95 | +4.2 | 0% | |
| 1 km | Open field | –108 | –1.5 | 2% | |
| 2 km | Open field | –120 | –8.0 | 15% | |
| 500 m | Urban | –103 | +1.0 | 5% | 2-story buildings |
| 200 m | Indoor | –118 | –5.5 | 30% | Through 3 walls |

**Pass Criteria:**
- Packet Loss < 5% at 1 km line-of-sight
- RSSI > –120 dBm at maximum tested range
- SNR > –10 dB for reliable demodulation

---

### Power Consumption Test Protocol

**Equipment:** INA219/226 in-circuit, USB power meter as reference, serial log.

**Test Conditions:**

| Mode | Description | Measurement Duration |
|---|---|---|
| Boot | From power-on to first HELLO packet | One-shot, peak current |
| BLE Active + LoRa RX | App connected, listening | 5 minutes average |
| LoRa TX Burst | Single packet transmission | Peak current, 400 ms window |
| BLE Disconnected | No BLE client, LoRa listening | 5 minutes average |
| Light Sleep | BLE off, between LoRa windows | 5 minutes average |

**Target Values:**

| Mode | Target Current |
|---|---|
| BLE Active + LoRa RX | < 200 mA |
| LoRa TX Peak | < 250 mA |
| BLE Disconnected | < 100 mA |
| Light Sleep | < 30 mA |

---

### ESP32 Subsystem Validation

**Pre-field checklist for each built node:**

| Test | Method | Expected Result |
|---|---|---|
| Boot test | Power on, observe serial | All subsystems initialize without error |
| LoRa TX test | Serial command `AT+TEST_TX` | Second node receives packet |
| BLE discovery | Android BLE scanner | Node advertised name visible |
| BLE connection | Android app connect | Serial shows `Client connected` |
| INA reading | Serial log | Voltage 3.28–3.35V, current > 0 |
| SPI integrity | LoRa register read | Register values match expected defaults |
| NVS persistence | Reboot, check serial | Same Node ID shown after reboot |

---

## Field Testing

### Multi-Node Mesh Test

**Configuration:** 3 nodes (A, B, C) placed linearly. Node B is the relay. Node A and Node C are out of direct LoRa range of each other.

```
[Node A] ──── 800m ──── [Node B relay] ──── 800m ──── [Node C]
```

**Test Scenarios:**

| Scenario | Method | Pass Criteria |
|---|---|---|
| 2-hop TEXT delivery | App A sends private message to Node C | Message appears on App C |
| 2-hop ACK | Node C receives message | App A shows Delivered status |
| Global broadcast propagation | App A sends GLOBAL_CHAT | Received on both B and C |
| Relay node offline | Power off Node B, send from A to C | Message stored; delivered when B returns |
| SOS multi-hop | App A sends SOS | Full-screen alert on App C |

---

### Emergency Scenario Test

**Simulated scenario:** Earthquake response, 3 teams at different positions.

**Setup:**
- 3 nodes deployed at 500 m spacing in urban area
- Each node connected to an Android phone with test user profile
- Team 1 is the "survivor" (SOS), Team 2 is relay, Team 3 is rescue command

**Test Script:**

1. Team 1 activates SOS — record time-to-receipt at Team 3
2. Team 3 sends LOCATION to Team 1 — verify coordinates on Team 1's map
3. Team 1 sends VOICE message to Team 3 — verify playback
4. All teams send RESOURCE packets — verify Resource Board aggregation
5. Power Bank on Team 2 node removed — verify store-and-forward activates
6. Power Bank reconnected — verify cached messages deliver

**Pass Criteria:**
- SOS received at command node within 5 seconds (2 hops)
- All message types transmit and display correctly
- Store-and-forward successfully delivers queued messages on node recovery

---

### Regression Testing

Before any firmware or app release, run the complete regression suite:

| Suite | Automated | Manual |
|---|---|---|
| Android Unit Tests | `./gradlew test` | — |
| Android Integration Tests | `./gradlew connectedAndroidTest` | — |
| Android UI Tests | `./gradlew connectedAndroidTest` | — |
| Firmware Unit Tests | `pio test -e native` | — |
| BLE Connection Stability | — | 30-minute connected session, no drops |
| 3-node mesh end-to-end | — | Per Multi-Node Mesh Test above |
| SOS scenario | — | Per Emergency Scenario Test above |
| Range validation | — | Per LoRa Range Test Protocol above |
