# Cryptographic & Security Threat Model — Phase A13

## Security Architecture

The Offline Emergency Mesh Messenger executes client-to-client cryptographic boundaries. Data payloads are encrypted before being enqueued or packed into packet frames:

```
  ┌───────────────────────────┐
  │     Plaintext Message     │
  └─────────────┬─────────────┘
                │ encrypt (AES-256-GCM)
  ┌─────────────▼─────────────┐
  │  Encrypted Payload Bytes  │
  └─────────────┬─────────────┘
                │ packaging
  ┌─────────────▼─────────────┐
  │    JSON Packet Wrapper    │
  └─────────────┬─────────────┘
                │ broadcast
  ┌─────────────▼─────────────┐
  │      LoRa Transceiver     │
  └───────────────────────────┘
```

---

## Cryptographic Architecture & Specifications

### 1. Symmetric Encryption (AES-256-GCM)
- **Algorithm**: `AES/GCM/NoPadding` (Galois/Counter Mode).
- **Key Size**: 256 bits (32 bytes).
- **IV Requirements**: 96 bits (12 bytes) generated using a cryptographically secure pseudo-random number generator (`java.security.SecureRandom`).
- **Nonces Rule**: Reusing an IV with the same AES key destroys security. A unique IV is generated for every single message.
- **Authentication**: GCM natively generates a 128-bit authentication tag appended to the ciphertext to ensure payload integrity and prevent tampering.

### 2. Key Exchange & Agreement (ECDH secp256r1)
- **Algorithm**: Elliptic Curve Diffie-Hellman (ECDH) on curve spec `secp256r1`.
- **Purpose**: Deriving shared session keys offline between two peer devices without requiring coordinator directory servers.
- **Derivation**:
  1. Peer A generates temporary keypair, sends Public Key A to Peer B.
  2. Peer B generates keypair, sends Public Key B to Peer A.
  3. Shared Secret = $ECDH(\text{Private Key A}, \text{Public Key B}) = ECDH(\text{Private Key B}, \text{Public Key A})$.
  4. The shared secret is passed to a Key Derivation Function (KDF) to output a 256-bit AES key.

### 3. Hashing (SHA-256)
- **Purpose**: Creating unique message fingerprints for deduplication check loops and integrity confirmation.

---

## Security Threat Model

The mesh architecture protects against the following attack scenarios:

| Threat | Description | Mitigation Strategy |
|---|---|---|
| **Eavesdropping (Sniffing)** | Passive listeners intercepting LoRa RF frequencies. | Payloads are encrypted with AES-256-GCM. Listeners read only random ciphertext bytes. |
| **Tampering (Modification)** | Attackers intercepting packets, altering coordinates/text, and re-transmitting. | Decryption checks the GCM authentication tag. If the ciphertext has been altered, the tag verification fails and the packet is dropped. |
| **Replay Attacks** | Capturing a distress message and rebroadcasting it later to cause confusion. | Each packet embeds a unique timestamp and nonce. Nodes reject packets outside acceptable time slots or with matching cached nonces. |
| **Node Spoofing** | Pretending to be a trusted responder or authority. | Future integration of signature signing using Device Identity Keypairs. |
| **Key Extraction** | Physical device extraction of private keys. | Keys are generated inside the **Android Keystore**, ensuring private keys are non-exportable from the secure hardware boundary (TEE/StrongBox). |
