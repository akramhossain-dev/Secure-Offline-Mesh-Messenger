# Privacy Model

The Offline Emergency Mesh Communication System treats privacy as a safety requirement. In emergency or civil situations, tracking location or metadata exposes users to threat actors.

---

## 1. Location Privacy Controls

### A. Explicit User Initiation
The system operates under a strict pull-based location access model:
* **No Background Location Tracking**: The application does not require the Android `ACCESS_BACKGROUND_LOCATION` permission.
* **On-Demand Polling**: Location is fetched only when the user explicitly clicks "Share Location" or triggers an SOS.
* **Location Share Expiry Timers**: Share payloads include an `expires_at` timestamp. Once this time is reached, the Android app ceases location broadcasts, and receiving devices mark the node's position as expired.

### B. Location Sourcing and Path
GPS coordinates are obtained via:
$$\text{Android GPS} \longrightarrow \text{Android Application} \longrightarrow \text{E2E Encrypted Packet} \longrightarrow \text{BLE / LoRa Mesh}$$
* **No ESP32 GPS hardware is used**.
* Transmitting location packets across the mesh network utilizes AES-256-GCM encryption. Non-recipient relay nodes see only ciphertext.

---

## 2. Permission Scopes

The application requests the following Android permission scopes:

| Permission Group | Purpose | Privacy Mitigation |
|---|---|---|
| **Location** | Fused Location access for maps and SOS coordinates. | Fine Location is accessed only while the app is in the foreground (except during an active SOS state run via a high-priority foreground service). |
| **Bluetooth** | BLE Scan & Connect to pair and interface with the ESP32 node. | No location parameters are derived from BLE scans. |
| **Microphone** | Audio record permission for voice clips. | The microphone is activated only when the user holds the record UI button. |
| **Notifications** | Alerting users to messages and SOS flags in the background. | Alerts are processed locally; no server push notifications are used. |

---

## 3. Network Discretion Controls

### A. BLE Visibility Settings
* **Public Mode**: The node advertises its Node ID via BLE, allowing nearby contacts to verify connectivity.
* **Private Mode**: BLE advertisements are disabled. The node acts as a passive receiver, forwarding mesh packets but remaining invisible to local BLE scanners.

### B. Plaintext Transparency
* **Global Chat**: Broadcast packets are sent in plaintext Base64 to allow open communication. Users are explicitly warned in the chat UI before composing these messages.
* **Private Chat**: Encrypted end-to-end. Relay nodes see only randomized headers.
