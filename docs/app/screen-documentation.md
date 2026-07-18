# Screen Documentation

This document describes all screens in the Android application, detailing their purpose, UI components, user actions, data requirements, and navigation paths.

---

## 1. Splash Screen
* **Purpose**: Serves as the application entry point, performing initial checks (identity presence, Bluetooth states) and branding.
* **UI Components**:
  * Brand Logo (Centered)
  * App Name (Emergency Mesh Messenger)
  * Loading Indicator / Status Text (e.g., "Initializing key store...")
* **User Actions**: None (Automated transition).
* **Data Required**: Identity record check in the Local Database.
* **Navigation**:
  * Navigates to **Onboarding Screen** if identity is not found.
  * Navigates to **Home Dashboard** if identity already exists.

---

## 2. Onboarding Screen
* **Purpose**: Explains the infrastructure-free mesh network concept to new users.
* **UI Components**:
  * Carousel/Pager detailing system capabilities (Bluetooth + LoRa range, Offline Maps, SOS features).
  * "Get Started" call-to-action button.
* **User Actions**:
  * Swipe through instructional slides.
  * Click "Get Started" to initiate setup.
* **Data Required**: Static text/images defining onboarding flow.
* **Navigation**: Navigates to **Identity Setup Screen** upon clicking "Get Started".

---

## 3. Identity Setup Screen
* **Purpose**: Configures the local user profile and generates device identity keys.
* **UI Components**:
  * Display Name input text field.
  * Profile avatar picker (Emoji/Unicode selectors).
  * Visibility toggle (Public/Private BLE advertisements).
  * "Generate Profile" button.
  * Progress/Loading dialog for cryptographic generation.
* **User Actions**:
  * Input name and choose avatar.
  * Click "Generate Profile".
* **Data Required**: Inputs from user. Triggers generation of UUID and ECDH keys.
* **Navigation**: Navigates to **Home Dashboard** once keys are safely generated and written to Android Keystore and Room DB.

---

## 4. Home Dashboard
* **Purpose**: The main application landing hub providing access to contacts, chat lists, and quick actions.
* **UI Components**:
  * Bottom navigation bar (Chats, Map, Contacts, Settings).
  * Active chat list showing conversation threads (Display name, status, snippet of last message, unread indicator).
  * Quick-trigger Floating Action Button (FAB) for SOS.
  * System connectivity status bar (Connected to ESP32 / Searching / BLE disabled).
* **User Actions**:
  * Tap conversation thread to open Chat.
  * Click navigation icons.
  * Press SOS FAB (requires confirmation).
* **Data Required**: Flow of conversation summaries from Room DB, current BLE status from Communication Manager.
* **Navigation**: Navigates to **Chat Screen**, **Global Chat Screen**, **Map Screen**, **Contacts Screen**, **Settings Screen**, or **Emergency Screen**.

---

## 5. Chat Screen
* **Purpose**: Enables E2E encrypted private conversations between two paired users.
* **UI Components**:
  * Recipient display header with status badge (Normal, Emergency, Rescue).
  * Chronological list of message bubbles (Right for sent, Left for received).
  * Message status indicators (Queued, Sent, Delivered via ACK).
  * Text input box.
  * Voice record button (hold to record).
  * Attachment menu (Share Location, Request Location, Share Resource).
* **User Actions**:
  * Type and send text.
  * Hold voice record button.
  * Attach location/resource data.
* **Data Required**: Active conversation entity, list of message objects, recipient's public key.
* **Navigation**: Back to **Home Dashboard**, or click recipient profile to view details.

---

## 6. Global Chat Screen
* **Purpose**: Enables unencrypted group discussions across the entire local LoRa mesh network.
* **UI Components**:
  * Global broadcast header with explicit warning: "All messages are public."
  * Message feed showing sender's name, avatar, message content, and hop count.
  * Text input and send controls.
* **User Actions**: Compose and broadcast message.
* **Data Required**: List of unencrypted global chat messages from Room database.
* **Navigation**: Back to **Home Dashboard**.

---

## 7. Emergency Screen
* **Purpose**: Dedicated control center for active SOS operations and emergency broadcasting.
* **UI Components**:
  * Large, pulsating red "SOS" button (inactive state) or "CANCEL SOS" (active state).
  * Countdown timer indicating repeat intervals (every 60 seconds).
  * Coordinate display (Latitude, Longitude, Accuracy).
  * "Distress Message" text box to customize SOS broadcast content.
* **User Actions**:
  * Trigger SOS with double confirmation.
  * Input custom distress message.
  * Cancel active SOS.
* **Data Required**: Phone GPS coordinates, user emergency status.
* **Navigation**: Can be accessed from any dashboard or chat screen via FAB or notification.

---

## 8. Map Screen
* **Purpose**: Displays offline cartographic data, plotting positions of contacts and active emergency markers.
* **UI Components**:
  * OsmDroid offline map container.
  * Zoom and center-to-device buttons.
  * Pulsing red pins for SOS alerts.
  * Green/Blue pins for active location-sharing contacts.
  * Location sharing configuration card (duration selections: 15m, 1h, until stopped).
* **User Actions**:
  * Pan and zoom map.
  * Tap pins to launch private chats.
  * Toggle location sharing.
* **Data Required**: OpenStreetMap offline tile packages, coordinate rows from `location_share` database, current phone GPS coordinates.
* **Navigation**: Back to **Home Dashboard**.

---

## 9. Network Dashboard
* **Purpose**: Monitors mesh health, signal parameters (RSSI/SNR), and queue status.
* **UI Components**:
  * Current Active Transport indicator (Bluetooth, LoRa, Store & Forward).
  * Active mesh node list showing recently heard nodes (HELLO packets) and hop count metrics.
  * Signal Quality panel (latest RSSI, SNR).
  * Outbound pending packet queue size indicator.
* **User Actions**:
  * Manually flush Store & Forward queue.
  * Probe/Ping nearby nodes (HELLO request).
* **Data Required**: Telemetry states from `CommunicationManager`, list of active nodes from Room DB.
* **Navigation**: Back to **Home Dashboard** or **Settings**.

---

## 10. Resource Sharing Board
* **Purpose**: Displays and manages resource offers (water, food, medicine, tools) throughout the mesh.
* **UI Components**:
  * Filter tabs (Water, Food, Medical, Shelter, Tools).
  * Feed of items showing: Resource, Quantity, Location Text, Expiry, Sender display name.
  * "Post Offer" dialog form.
* **User Actions**:
  * Post a resource availability offer.
  * Tap item to initiate direct E2E conversation with poster.
* **Data Required**: Active rows from `resource` database table.
* **Navigation**: Back to **Home Dashboard**.

---

## 11. Settings Screen
* **Purpose**: Adjusts user profile parameters, app features, and database states.
* **UI Components**:
  * Display name and avatar editor.
  * BLE visibility setting (Public/Private).
  * Messaging configuration (TTL slider, Store & Forward toggle).
  * Power telemetry selector (enable/disable live monitoring).
  * "Clear All Data" action button.
* **User Actions**:
  * Adjust configurations.
  * Reset paired hardware node connection.
  * Factory reset local Room DB.
* **Data Required**: User configurations mapped to database preferences.
* **Navigation**: Back to **Home Dashboard**.
