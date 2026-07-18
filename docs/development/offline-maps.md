# Location & Offline Map Foundation — Phase A16

## Location Architecture

The location system operates offline, storing coordinate updates locally inside the SQLite database before sharing with peers:

```
 ┌───────────────────────────┐
 │     Location Provider     │  (GPS/Network wrapper)
 └─────────────┬─────────────┘
               │ LocationData
 ┌─────────────▼─────────────┐
 │    Location Repository    │  (Manages history and sharing status)
 └─────────────┬─────────────┘
               │ LocationEntity
 ┌─────────────▼─────────────┐
 │       Room Database       │  (Persists to local disk)
 └───────────────────────────┘
```

---

## Location Data Schema Specifications

Coordinate records represent a standard format across transceivers:
- **Location ID**: Prefixed unique string key.
- **Latitude / Longitude**: WGS84 double values.
- **Altitude**: Double precision value (needed for mountainous search and rescue operations).
- **Accuracy**: Float value (meters radius precision).
- **Timestamp**: Epoch time.
- **Provider**: Provider class source identifier (`gps`, `network`, `external`).
- **Device ID**: Originating client hardware identifier.

---

## Offline Maps & Tiling Strategy

Disaster zones lack access to Google Maps or Mapbox CDNs. The mapping subsystem utilizes local tiles:

1. **Map Abstraction**: Managed via the [`MapProvider`](../../android/app/src/main/java/com/mesh/emergency/core/map/MapProvider.kt) interface to allow drop-in replacements for future GIS renderers.
2. **MBTiles Packages**: Map vectors and raster images are packed into single-file SQLite databases (`.mbtiles`) stored on local SD cards or external storage.
3. **Map Layers**:
   - `topo_layer`: Essential geographical elevation and water structures vector lines.
   - `rescue_nodes_layer`: Dynamic layer placing active LoRa relays.
   - `sos_markers_layer`: Placed beacons coordinates markers.

---

## Privacy & Location Controls

Because tracking coordinates can compromise privacy:
- **No Background Tracking**: Location loops only execute while the app is active or when the operator explicitly turns on Emergency mode.
- **Permission Boundary**: System services check coordinates permission before enabling the GPS chip.
- **Visibility Toggles**: Users can toggle "Sharing Status" to prevent sending their location packets during standard text chat hops, keeping GPS logs strictly inside local DB records.
