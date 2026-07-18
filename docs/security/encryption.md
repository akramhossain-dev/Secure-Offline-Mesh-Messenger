# Encryption

This document details the E2E encryption architecture and primitives used across the Android app and the ESP32 firmware nodes.

---

## 1. Key Exchange (X25519 / ECDH)

To establish E2E encryption without a central authority:
1. When two users scan each other's QR codes, they exchange their static **X25519 (ECDH)** public keys.
2. Each user computes a shared secret locally:
   $$\text{Shared Secret} = \text{ECDH}(\text{Local Private Key}, \text{Remote Public Key})$$
3. An HKDF-SHA256 derivation function generates the 256-bit symmetric session key from this shared secret:
   $$\text{Session Key} = \text{HKDF}(\text{Shared Secret}, \text{"mesh-session-v1"})$$

---

## 2. Symmetric Encryption (AES-256-GCM)

All private text and voice message payloads are encrypted with **AES-256-GCM**:
* **IV (Initialization Vector)**: A cryptographically secure random 12-byte initialization vector is generated for each packet.
* **Authentication Tag**: GCM generates a 16-byte authentication tag ensuring payload integrity.
* **Format**: The payload payload is packed as:
  $$\text{Payload} = \text{IV (12 bytes)} \parallel \text{Ciphertext} \parallel \text{Tag (16 bytes)}$$

```kotlin
// Android encryption flow example
val cipher = Cipher.getInstance("AES/GCM/NoPadding")
val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
cipher.init(Cipher.ENCRYPT_MODE, sessionKey, GCMParameterSpec(128, iv))
val ciphertext = cipher.doFinal(plaintext.toByteArray())
val outputPayload = iv + ciphertext
```

---

## 3. ESP32 Cryptographic Capabilities

The Semtech SX1278 LoRa module does not support hardware encryption. All processing occurs on the ESP32 microcontroller:
* **mbedTLS**: The ESP32 firmware utilizes the standard `mbedtls` library integrated into ESP-IDF.
* **Hardware RNG**: The firmware seeds cryptographic generators using the ESP32 hardware random number generator (`esp_random()`).

---

## 4. Integrity Verification (HMAC-SHA256)

Every packet carries a Base64-encoded signature covering the packet's canonical header variables (`id + sender + receiver + timestamp + payload`):
* The signature is generated using a SHA-256 HMAC key derived from the sender's private key.
* Receiving nodes verify this HMAC signature using the sender's public key before forwarding or displaying the packet.
* Any modified packet fails validation and is discarded.
