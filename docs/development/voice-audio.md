# Voice Message & Audio Communication — Phase A20

## Voice Message Architecture

Voice messages represent high-fidelity data that must be compressed aggressively to transit over low-bandwidth LoRa links offline:

```
 ┌───────────────────────────┐
 │       Voice Manager       │  (Coordinates queues and properties)
 └─────────────┬─────────────┘
               │ LocationData
 ┌─────────────▼─────────────┐
 │       Audio Provider      │  (Microphone stream and storage hooks)
 └─────────────┬─────────────┘
               ├───────────────────────┐
 ┌─────────────▼─────────────┐   ┌─────▼─────────────────────┐
 │    Local SQLite Storage   │   │   Store & Forward Queue   │
 └───────────────────────────┘   └───────────────────────────┘
```

The app handles voice logs through the [`VoiceManager`](../../android/app/src/main/java/com/mesh/emergency/core/audio/VoiceManager.kt) wrapper, storing file indexes inside Room [`VoiceMessageEntity`](../../android/app/src/main/java/com/mesh/emergency/data/local/entity/VoiceMessageEntity.kt) rows.

---

## Audio Compression Strategy

Because LoRa transceivers transfer bytes at very low speeds (e.g. 500 bps to 5 kbps), the voice subsystem forces audio compression profiles:

- **`NORMAL` Quality**:
  - Codec: **Opus** or **AAC**.
  - Bitrate: 16 kbps (excellent for standard direct chat over BLE).
- **`SAVING` Quality**:
  - Bitrate: 8 kbps (mono channel).
- **`EMERGENCY` Quality**:
  - Codec: **MELPe** or Ultra-low-bitrate **Opus**.
  - Bitrate: **1.2 kbps to 2.4 kbps** (crucial for forwarding small, intelligible speech packets over LoRa mesh networks).

---

## Voice Message Database Schema

The [`VoiceMessageEntity`](../../android/app/src/main/java/com/mesh/emergency/data/local/entity/VoiceMessageEntity.kt) tracks:
- **Voice ID**: Unique identifier prefix.
- **File Reference**: Local absolute path pointing to the cached `.opus` file on internal disk.
- **Duration**: Playback duration in milliseconds.
- **File Size**: Raw size in bytes.
- **Format**: File container metadata tag (`opus`, `aac`, `amr`).
- **Quality**: Compression mode marker.
- **Status**: Pipeline queue tracking state (`PENDING`, `QUEUED`, `FAILED`, `EXPIRED`).

---

## Store & Forward Messaging Integration

Voice packets are split into multiple smaller chunk frames before transmission over LoRa. The forwarding engine incorporates:
1. **Incremental Retries**: Fails over gracefully, retrying failed chunks.
2. **Chunk Checksums**: Incorporates checksum checks to prevent rebuilding corrupted voice clips.
3. **Short TTLs**: Voice files carry shorter lifetimes (e.g. 24 hours) compared to coordinate events, preventing mesh nodes from exhausting memory cache tables.
