# Future Improvements

This document lists potential feature enhancements and hardware upgrades for future releases of the system.

---

## 1. Hardware Enhancements

* **GPS Integration**:
  * Add support for low-power GPS modules (e.g., Neo-6M) on the ESP32 node.
  * This would allow the node to fetch location coordinates autonomously, removing the dependency on mobile device GPS.
* **Alternate Frequencies**:
  * Implement support for **868 MHz** and **915 MHz** bands to comply with regional regulations in Europe and North America.
* **Secondary Bluetooth Link**:
  * Support BLE connection sharing, allowing multiple nearby Android devices to connect to a single ESP32 bridge node.

---

## 2. Protocol and Routing Optimization

* **Dynamic Spreading Factor**:
  * Adjust the Spreading Factor dynamically based on link quality (RSSI/SNR) to balance throughput and range.
* **Frequency Hopping**:
  * Implement Frequency Hopping Spread Spectrum (FHSS) to improve security and reduce interference in congested zones.
* **Compressed Audio Codecs**:
  * Evaluate advanced low-bitrate codecs (e.g., Codec2) to reduce voice message size and airtime requirements.

---

## 3. Application Enhancements

* **iOS Application Support**:
  * Develop a companion iOS application using Kotlin Multiplatform (KMP) to share data and presentation logic.
* **Offline Map Layer Sourcing**:
  * Enable importing custom offline map packages (MBTiles) directly from the device's storage.
* **Encrypted Backups**:
  * Support exporting local chat histories and contact lists as encrypted backup files to external storage.
