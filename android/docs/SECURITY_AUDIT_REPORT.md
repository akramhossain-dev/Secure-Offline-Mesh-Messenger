# Security Audit & Hardening Report
**Offline Emergency Mesh Communication System — Phase A34**

## 1. Security Architecture

The application implements a decentralized, offline-first security architecture designed to operate with zero dependencies on cloud authentication, central certificate authorities, or internet availability.

```
                  [ Offline Security Boundaries ]
                  
 ┌─────────────────────────────────────────────────────────────┐
 │                      Android Application                    │
 │                                                             │
 │   ┌─────────────────┐             ┌─────────────────────┐   │
 │   │   User Space    │             │   Security Module   │   │
 │   │                 │             │                     │   │
 │   │  Message History│  Encrypted  │   Cryptography      │   │
 │   │  Location Pins  │◄───────────►│   Engine            │   │
 │   │  Resource Shares│  (AES-GCM)  │   (AES-256-GCM)     │   │
 │   └────────┬────────┘             └──────────┬──────────┘   │
 │            │                                 │              │
 │            ▼                                 ▼              │
 │   ┌─────────────────┐             ┌─────────────────────┐   │
 │   │  Local Database │             │     Key Manager     │   │
 │   │  (allowBackup=  │             │                     │   │
 │   │   false)        │             │   ECDH Derivation   │   │
 │   └─────────────────┘             └──────────┬──────────┘   │
 │                                              │              │
 └──────────────────────────────────────────────┼──────────────┘
                                                │ Local Private
                                                ▼ Keys
                                     ┌─────────────────────┐
                                     │  Android KeyStore   │
                                     │  (Hardware-backed)  │
                                     └─────────────────────┘
```

The system comprises three main layers:
1. **Cryptography Engine** (`CryptographyEngine`): Executes fast authenticated symmetric encryption (AES-256-GCM) and SHA-256 hashing.
2. **Key Manager** (`KeyManager`): Orchestrates Elliptic Curve Diffie-Hellman (ECDH) key agreements over the `secp256r1` curve to dynamically derive secret keys.
3. **Key Storage** (`KeyStorage`): Leverages the Android Keystore system to securely generate a master AES key used to encrypt raw private/public key records.

---

## 2. Threat Model

| Threat | Target | Mitigation | Status |
|--------|--------|------------|--------|
| **Physical Device Theft** | SQLite DB file exposure | Disabled auto-backup (`allowBackup="false"`) and isolated DB in internal application sandbox. | Mitigated |
| **Interception / Eavesdropping** | Mesh BLE Packets | All message payloads are encrypted with AES-256-GCM derived via ECDH. | Mitigated |
| **Identity Impersonation** | Peer Identity Key | Identity key pairs are generated locally via hardware-backed KeyStore and never shared over the air. | Mitigated |
| **Reverse Engineering** | Debug logs leakage | Logging is stripped via `ReleaseTree` in release mode, and hardcoded mock fallbacks are completely removed. | Mitigated |
| **Replay Attacks** | Location sharing packets | Location packets contain high-resolution UNIX timestamps and accurate sender device IDs. | Mitigated |

---

## 3. Privacy Policy

The system is built on **Privacy by Design** principles for disaster recovery scenarios:
- **Zero Internet Access:** The application does not request `android.permission.INTERNET` or connect to external servers. All operations are strictly local.
- **Granular Permissions:** Requests permissions (BLE, Location, Microphone) only when features are activated, and declares `usesPermissionFlags="neverForLocation"` to assure users that Bluetooth scanning is not used to track geographic positions without consent.
- **Data Erasure Rights:** Users can purge selective or all data (database tables, paired devices, and cryptographic identity keys) instantly via the Settings UI.

---

## 4. Encryption Guide

### End-to-End Encryption (E2EE) Pipeline
1. **Identity Setup:** Device generates a unique `secp256r1` Elliptic Curve keypair.
2. **Handshake:** When exchanging identity cards (via QR codes or BLE advertisements), the public keys are exchanged.
3. **Secret Derivation:** Both devices execute an ECDH key agreement:
   $$\text{Shared Secret} = \text{ECDH}(\text{Local Private Key}, \text{Remote Public Key})$$
4. **Symmetric Encryption:** Payload is encrypted using AES-256-GCM with a 12-byte random Initialization Vector (IV).
5. **Decryption:** Receiver uses the derived secret key and the package IV to decrypt the authenticated ciphertext.

---

## 5. Security Hardening Checklist

- [x] **No Static Keys:** Hardcoded mock keys completely removed from production.
- [x] **Keystore Encryption:** Asymmetric private keys are encrypted on-disk using a master key protected inside the hardware-backed Android KeyStore.
- [x] **Secure Purge:** Added full table wipe (`clearAllTables()`) and cryptographic KeyStore deletion to comply with user erasure settings.
- [x] **Input Validation:** Parameterized Room queries to eliminate SQLite injection vectors.
- [x] **Uncaught Crash Handling:** Configured `CrashHandler` to safely trap system failures and avoid exposing core dumps to local logs.
- [x] **Logging Auditing:** Discarded all logs containing encryption keys or plaintext messages.
