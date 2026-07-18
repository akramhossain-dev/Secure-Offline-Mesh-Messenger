# Identity Management

This document details the identity structure and storage mechanisms for user profiles and hardware connections.

---

## 1. Identity Hierarchy

The system defines identity at two distinct layers:

### A. User Identity (Android Level)
* **Node ID**: A UUID generated locally on first launch.
* **ECDH Key Pair**: Used for E2E encryption key agreement.
* **Signature Key Pair**: Used for HMAC-SHA256 packet signatures.
* **Display Name & Avatar**: Local configurations shared during contact pairing.

### B. Device Identity (ESP32 Level)
* **MAC Address**: The Bluetooth BLE physical address.
* **Local Identity Key**: An internal identity key pair generated on the node on first boot and stored in the ESP32 NVS encrypted partition.

---

## 2. Key Storage (Android Keystore)

To prevent the extraction of private keys from compromised or rooted Android devices, the application stores keys within the **Android Keystore System**:
* **Hardware-Backed Storage**: Keys are generated within a Trusted Execution Environment (TEE) or StrongBox key store.
* **No Key Leakage**: Private keys remain within the hardware boundary. The application passes input data to the Keystore API for cryptographic operations, and receives the output signature or decrypted payload.

---

## 3. Secure Pairing Flow

To prevent man-in-the-middle (MITM) attacks, contact creation requires a visual QR code scan:

```
[User A opens Profile]             [User B opens Scanner]
          │                                  │
          ▼                                  ▼
     Display QR                          Scan QR
(NodeID:PublicKey)                           │
                                             ▼
                                   Store A's Public Key
                                             │
                                             ▼
                                  Send HELLO packet over BLE
                                  (User B's NodeID:PublicKey)
          │                                  │
          ▼                                  │
    Receive HELLO ◄──────────────────────────┘
          │
          ▼
Store B's Public Key
```

* Because the QR code transfer is local and direct, an attacker cannot intercept or alter public keys during the exchange.
* A contact's public key is bound permanently to their Node ID in the local database. If an attacker broadcasts packets claiming to be from User A using a different key, the signature verification fails.
