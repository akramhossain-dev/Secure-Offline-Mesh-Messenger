# User & Device Identity System — Phase A6

## Core Architectural Concepts

An offline emergency mesh network requires strict isolation of entities. The architecture distinguishes between four primary concepts:

| Term | Domain | Definition | Persistence Target |
|---|---|---|---|
| **User** | Application Profile | The local operator using the application. Identifies a single human responder or citizen. | `users` table (`isCurrentUser = 1`) |
| **Device** | Local Hardware Platform | The physical smartphone hosting the application. Identified by non-sensitive platform signatures. | `devices` table |
| **Node** | Network Interface Bridge | The ESP32 or LoRa hardware device bridging the smartphone to the mesh network. | `network_nodes` table |
| **Contact** | Peer Responder Profile | A remote user's identity synced via BLE/LoRa/QR handshake. Includes trust status tags. | `users` table (`isCurrentUser = 0`) |

---

## Offline ID Generation Strategy

Since the system operates in disaster areas without cellular/Internet access, all identifiers must be generated locally without coordinate servers:

- **Abstracation**: [`IdentityGenerator`](../../android/app/src/main/java/com/mesh/emergency/core/identity/IdentityGenerator.kt)
- **Collision-Resistance**: Implemented via UUID Version 4 (`java.util.UUID`), yielding $2^{122}$ random bits. The probability of collision is negligible even under high network node density.
- **Prefixing Rules**: Generated strings prepend standard indicators to simplify validation:
  - User: `usr_<uuid>`
  - Device: `dev_<uuid>`
  - Node: `nod_<uuid>`

---

## Privacy Model & Fingerprinting

Emergency communication systems must protect responders and citizens. The system enforces strict privacy safeguards:

### 1. Privacy Safeguards
- **Anonymity by Default**: No email, phone number, SIM card access, or real name registration is required to boot the application. Users can operate under temporary responder nicknames.
- **Data Minimization**: Location tracking is cached locally on the device and is never shared unless coordinates sharing permission is explicitly enabled by the user during distress alerts.
- **Transceiver Visibility Control**: Users can configure local transceivers to "invisible" standby modes, preventing nearby nodes from recording MAC presence.

### 2. Device Fingerprinting
To prevent node spoofing and manage encryption keys securely, a device signature is computed:
- **Abstraction**: [`DeviceFingerprintProvider`](../../android/app/src/main/java/com/mesh/emergency/core/identity/DeviceFingerprintProvider.kt)
- **Calculation**: Computes a SHA-256 hash combining Android Secure ID (`Settings.Secure.ANDROID_ID`) and generic system parameters (`Build.BRAND`, `Build.MODEL`, `Build.HARDWARE`).
- **Cryptographic Security**: The raw ANDROID_ID is hashed locally. Reversing the fingerprint to recover hardware details is impossible.
