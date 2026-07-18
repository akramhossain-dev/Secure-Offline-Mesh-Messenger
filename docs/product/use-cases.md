# Use Cases

**Project:** Offline Emergency Mesh Communication System  

---

## Actor Definitions

| Actor | Description |
|---|---|
| **Survivor** | A civilian in a disaster or emergency zone using the app |
| **Rescue Operator** | A trained first responder or search and rescue team member |
| **Coordinator** | An incident commander or NGO leader managing the response |
| **Field Worker** | A researcher, guide, or remote worker using the mesh for routine communication |
| **Relay Node** | An unattended ESP32 node that participates in mesh routing only |

---

## UC-01: First-Time Identity Setup

**Actor:** Any user (first launch)  
**Goal:** Establish a persistent identity on the device  

**Preconditions:** App installed; no prior identity exists.

**Flow:**
1. App generates a UUID Node ID locally
2. App generates an ECDH key pair; private key stored in Android Keystore
3. User enters a display name
4. User optionally sets avatar (emoji) and language preference
5. App stores identity in Room Database
6. App displays the profile QR code ready for pairing

**Postconditions:** Identity is created; user can send and receive messages.

**Error Cases:**
- Key generation fails → Retry with error toast; cannot proceed without identity

---

## UC-02: Pair with a Contact via QR Code

**Actor:** Two users (User A and User B)  
**Goal:** Establish a trusted contact with E2E key exchange  

**Preconditions:** Both users have identities; both devices have the app open.

**Flow:**
1. User A opens Profile screen → their QR code is displayed
2. User B opens Contacts → taps "Scan QR"
3. User B scans User A's QR code
4. App parses `NodeID:PublicKey` from QR
5. App adds User A to User B's contact list with their public key
6. User B's app sends a HELLO packet containing User B's `NodeID:PublicKey`
7. User A receives the HELLO, adds User B to their contacts
8. Mutual contact established; private chat available

**Postconditions:** Both users can send E2E encrypted messages to each other.

**Error Cases:**
- Invalid QR format → Error dialog; scan again
- Contact already exists → Update public key if changed; notify user

---

## UC-03: Send a Private Message

**Actor:** User (sender)  
**Goal:** Deliver an E2E encrypted text message to a specific contact  

**Preconditions:** Contact paired; Communication Manager running.

**Flow:**
1. User opens a conversation with a contact
2. User types a message and taps Send
3. `SendMessageUseCase` is invoked
4. Message is encrypted with the recipient's public key (AES-256-GCM)
5. Communication Manager evaluates transport:
   - If receiver is in BLE range → Bluetooth Transport
   - If receiver is on LoRa mesh → LoRa Transport
   - If receiver is offline → Store & Forward queue
6. Delivery status updates: QUEUED → SENT
7. ACK received → status updates to DELIVERED

**Postconditions:** Message stored in Room DB; delivery status visible in UI.

**Variations:**
- UC-03a: Voice message — audio recorded, chunked, sent as VOICE packets
- UC-03b: Long message — payload split across multiple packets with sequence numbers

---

## UC-04: Send a Global Broadcast

**Actor:** Any user  
**Goal:** Send a message visible to all mesh participants  

**Preconditions:** User is connected to at least one ESP32 node.

**Flow:**
1. User opens Global Chat screen
2. User types a message and taps Send
3. Packet type set to `GLOBAL_CHAT`; receiver set to `BROADCAST`
4. Packet is not encrypted (documented to user)
5. Communication Manager sends via LoRa transport
6. All nodes in mesh receive and display the message

**Postconditions:** All reachable nodes display the global message.

---

## UC-05: Trigger SOS

**Actor:** Survivor (in emergency)  
**Goal:** Broadcast a distress signal with GPS location to the entire mesh  

**Preconditions:** Location permission granted; app is open.

**Flow:**
1. User taps the SOS button from any screen
2. Confirmation dialog shown ("Are you sure?")
3. User confirms
4. App captures GPS coordinates from phone
5. SOS packet created with `priority = CRITICAL`, coordinates, battery level
6. Packet broadcast across entire mesh (TTL bypassed for first 2 hops)
7. User status set to EMERGENCY
8. SOS repeat worker starts (every 60 seconds)
9. Active SOS screen displayed with cancel option

**Postconditions:** All reachable nodes alert their users; rescue contacts notified.

**Cancel Flow:**
1. User taps Cancel SOS
2. Repeat worker stopped
3. User status reset to Normal
4. Cancellation packet broadcast

---

## UC-06: Receive and Respond to an SOS

**Actor:** Rescue Operator  
**Goal:** Acknowledge an SOS and coordinate response  

**Preconditions:** Node receives an SOS packet.

**Flow:**
1. SOS received → full-screen alert displayed on all nearby devices
2. Recipient sees sender's name, GPS coordinates, and distress message
3. Rescue Operator opens the map view — SOS sender marked with red pulsing icon
4. Operator taps "Send Response" → private message sent to SOS sender
5. Operator can set their own status to RESCUE

**Postconditions:** SOS sender is aware a responder is coming.

---

## UC-07: Share Location

**Actor:** Any user  
**Goal:** Voluntarily share GPS position with contacts for a limited time  

**Preconditions:** Location permission granted; contacts available.

**Flow:**
1. User opens Map screen
2. User taps "Share My Location"
3. User selects duration (15 min / 1 hour / Until stopped)
4. App captures GPS coordinates
5. LOCATION packet sent to selected contacts (or broadcast)
6. Timer starts; location resent every 60 seconds until expiry
7. Sharing badge shown in status bar with countdown

**Postconditions:** Contacts see user's position on their offline maps.

---

## UC-08: Discover Nearby Devices

**Actor:** Any user  
**Goal:** Find and add new contacts from nearby ESP32 nodes  

**Preconditions:** Bluetooth enabled; ESP32 node nearby.

**Flow:**
1. User opens Contacts → taps "Discover Nearby"
2. App scans for BLE advertisements from ESP32 nodes
3. Discovered nodes appear as cards with Node ID and display name (from HELLO packets)
4. User taps a discovered node → options: "Add Contact", "Pair via QR"
5. If "Add Contact" without QR → contact added as Discovered (no encryption key)
6. If "Pair via QR" → UC-02 flow

**Postconditions:** New contacts available in the contact list.

---

## UC-09: Share Emergency Resources

**Actor:** Any user  
**Goal:** Announce available resources (food, water, shelter, medical) to the mesh  

**Preconditions:** Connected to the mesh network.

**Flow:**
1. User opens Resource Board → taps "Add Resource"
2. User fills in: resource type, quantity, location (text or GPS), expiry time
3. RESOURCE packet broadcast across mesh
4. All receiving nodes aggregate it on their Resource Board
5. Expiry timestamp auto-removes the resource after the set time

**Postconditions:** All mesh participants can see the resource offer.

---

## UC-10: Monitor Network Status

**Actor:** Coordinator / any user  
**Goal:** View the health and topology of the mesh network  

**Preconditions:** Connected to at least one ESP32 node.

**Flow:**
1. User opens Network Dashboard
2. Dashboard displays:
   - Active nodes (from recent HELLO packets)
   - Current transport (Bluetooth / LoRa / Store & Forward)
   - Message queue depth
   - Delivery stats (Delivered / Failed / Pending)
   - Signal quality (RSSI / SNR from last LoRa packet)
3. User can tap a node to see its last-known status and distance

**Postconditions:** User has situational awareness of the mesh network.

---

## UC-11: Store & Forward Delivery (Background)

**Actor:** System (no user action)  
**Goal:** Deliver cached messages when a previously offline receiver reconnects  

**Preconditions:** Message cached in `pending_packet` table; receiver offline.

**Flow:**
1. Destination node was offline when message was originally sent
2. Message stored in Room `pending_packet` with retry metadata
3. Background worker polls every 10 seconds
4. A HELLO packet is received from the destination Node ID
5. Worker retrieves cached packets for that receiver
6. Packets re-transmitted via LoRa Transport
7. ACK received → `pending_packet` entry deleted
8. Delivery status updated to DELIVERED in UI

**Postconditions:** User sees delivery confirmation; message not lost.

---

## UC-12: Monitor Power Consumption

**Actor:** Any user  
**Goal:** View real-time power telemetry from the ESP32 node  

**Preconditions:** Connected to an ESP32 node with INA219 sensor.

**Flow:**
1. User opens Power Monitor screen
2. App reads INA219 telemetry from BLE Status characteristic
3. Displays: bus voltage, current draw, power consumption, device health
4. Readings refresh every 5 seconds

**Postconditions:** User can estimate remaining power bank runtime.

---

## Related Documents

- [User Flow](user-flow.md)
- [Product Requirements](product-requirements.md)
- [Features](../overview/features.md)
