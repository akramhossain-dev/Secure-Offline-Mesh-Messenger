# Security Overview

The Offline Emergency Mesh Communication System is designed to operate securely in hostile, infrastructure-free environments. Security cannot rely on internet connectivity, external certificate authorities (CAs), or cloud servers.

---

## 1. Threat Model

| Threat | Target | Mitigation Strategy |
|---|---|---|
| **Eavesdropping** | Private text/voice traffic | End-to-End Encryption (AES-256-GCM + X25519) |
| **Impersonation** | Identity spoofing | Public Key pairing via QR Code |
| **Tampering** | Packet modification | HMAC-SHA256 Signatures |
| **Replay Attacks** | Old packet playback | Timestamp checks and seen packet caches |
| **Interception** | BLE access links | AES-256 encrypted BLE transport link |

---

## 2. Directory Index

Refer to the following security sub-documents for specialized details:

* [Encryption](encryption.md): Algorithms, library integrations, and E2E cryptographic implementation details.
* [Identity Management](identity-management.md): Generation, storage, and QR pairing rules.
* [Privacy Model](privacy-model.md): Detailed location protection features, visibility toggles, and background access boundaries.

---

## 3. Standard Cryptographic Algorithms

The application relies on secure, standard cryptographic primitives:

* **Symmetric Encryption**: AES-256-GCM for E2E payloads.
* **Key Agreement**: X25519 (ECDH) for key agreement.
* **Integrity/Authentication**: HMAC-SHA256.
* **Hashing**: SHA-256.
* **Randomness**: Android `SecureRandom` (CSPRNG) and ESP32 hardware entropy generators.
