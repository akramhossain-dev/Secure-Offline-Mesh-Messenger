package com.mesh.emergency.feature.message.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mesh.emergency.core.communication.PairingService
import com.mesh.emergency.core.discovery.qr.QRHandshakeData
import com.mesh.emergency.core.discovery.qr.QRHandshakeManager
import com.mesh.emergency.data.local.LocalDataSource
import com.mesh.emergency.data.local.entity.DbTrustStatus
import com.mesh.emergency.data.local.entity.DeviceEntity
import com.mesh.emergency.data.local.entity.UserEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel managing the QR pairing/scanning logic.
 * Decodes the public key handshakes and updates local DB entities.
 * After saving the scanned contact, sends a reverse handshake via BLE so the
 * remote device auto-pairs back — no second QR scan needed.
 *
 * Reverse handshake strategy (3-path + pending queue + BLE reconnect):
 *  1. Server notify  — if Phone B is already a GATT client connected to Phone A's server.
 *  2. Client write   — if Phone A is already a GATT client connected to Phone B's server.
 *  3. Direct GATT    — if Phone B's BLE MAC is real (not 02:00:00:00:00:00).
 *  4. Pending + scan — queues payload and triggers a BLE reconnect scan; delivers on connection.
 */
@HiltViewModel
class QrPairViewModel @Inject constructor(
    private val localDataSource: LocalDataSource,
    private val pairingService: PairingService
) : ViewModel() {

    private val _effect = MutableSharedFlow<QrPairUiEffect>()
    val effect: SharedFlow<QrPairUiEffect> = _effect.asSharedFlow()

    /**
     * Decodes QR code handshake JSON string and persists the contact and device profiles.
     * Then sends a reverse handshake so the remote device auto-pairs back.
     */
    fun processHandshakePayload(payload: String) {
        viewModelScope.launch {
            try {
                // ── QR_FLOW ───────────────────────────────────────────────────────────
                Timber.d("QR_FLOW: QR scanned — raw payload length=${payload.length}")
                Timber.d("QR_FLOW: Raw payload = $payload")

                // 1. Decode handshake JSON
                val data = QRHandshakeManager.parsePayload(payload)
                Timber.d("QR_FLOW: QR payload parsed — userId=${data.userId} username='${data.username}' deviceId=${data.deviceId} ble=${data.bleAddress}")
                Timber.d("QR_FLOW: Peer identity extracted — userId=${data.userId} publicKey=${data.publicKeyRef.take(20)}...")

                // Resolve the display name: prefer the name from QR, fall back to short ID.
                val contactName = data.username.ifBlank { "Contact-${data.userId.take(6)}" }

                // ── DATABASE ──────────────────────────────────────────────────────────
                // 2. Map and insert User (Contact) Entity
                //    entityId = userId (stable identity; same key used in reverse path)
                val user = UserEntity(
                    entityId = data.userId,
                    username = contactName,
                    profileImageRef = null,
                    languagePreference = "en",
                    createdTime = System.currentTimeMillis(),
                    updatedTime = System.currentTimeMillis(),
                    status = "ACTIVE",
                    isCurrentUser = false,
                    lastSeen = System.currentTimeMillis(),
                    trustedStatus = true,
                    nickname = contactName,
                    publicKey = data.publicKeyRef
                )

                // 3. Map and insert Device Entity
                //    entityId = userId for consistency with the reverse handshake path
                val device = DeviceEntity(
                    entityId = data.userId,
                    name = contactName,
                    rssi = -55,
                    lastSeen = System.currentTimeMillis(),
                    deviceType = data.deviceType,
                    platformInfo = "ANDROID",
                    createdTime = System.currentTimeMillis(),
                    lastActiveTime = System.currentTimeMillis(),
                    trustStatus = DbTrustStatus.TRUSTED,
                    nickname = contactName,
                    bleAddress = data.bleAddress
                )

                Timber.d("DATABASE: Before saving peer — userId=${data.userId} name='$contactName'")
                localDataSource.insertUser(user)
                Timber.d("DATABASE: After saving peer — UserEntity inserted id=${user.entityId} username='${user.username}'")
                localDataSource.insertDevice(device)
                Timber.d("DATABASE: After saving peer — DeviceEntity inserted id=${device.entityId} ble=${device.bleAddress}")

                // Query to confirm the row is in DB
                val savedDevices = try {
                    localDataSource.getDevices().firstOrNull()
                } catch (e: Exception) { null }
                Timber.d("DATABASE: Query paired devices list — total devices in DB: ${savedDevices?.size ?: "error"}")

                // ── PAIR_FLOW ─────────────────────────────────────────────────────────
                // 4. Send reverse handshake to the scanned device so it auto-pairs back
                launch {
                    try {
                        // Build our own identity for the reverse handshake JSON
                        val localProfile = localDataSource.getCurrentUser().firstOrNull()
                        val localUserId  = localProfile?.entityId ?: java.util.UUID.randomUUID().toString()
                        val localPubKey  = localProfile?.publicKey ?: ""
                        val localName    = localProfile?.username ?: ""
                        val localBle     = pairingService.localBleAddress

                        val reverseJson = JSONObject().apply {
                            put("type", "REVERSE_HANDSHAKE")
                            put("uid",  localUserId)
                            put("un",   localName)
                            put("pub",  localPubKey)
                            put("ble",  localBle)
                            put("dt",   "SMARTPHONE")
                        }.toString()

                        val reversePayload = reverseJson.toByteArray(Charsets.UTF_8)
                        Timber.d("PAIR_FLOW: Pair request created — localUid=$localUserId localName='$localName' targetBle=${data.bleAddress}")

                        // Three-path delivery:
                        // path1 serverNotify → path2 clientWrite → path3 directGATT → path4 pending+scan
                        pairingService.queueAndDeliverReverseHandshake(reversePayload, data.bleAddress)
                        Timber.d("PAIR_FLOW: Pair request sent — delivery attempted via all available BLE paths")
                    } catch (e: Exception) {
                        // Reverse handshake is best-effort; failure doesn't break forward pairing
                        Timber.e(e, "PAIR_FLOW: Pair request send failed (non-fatal)")
                    }
                }

                Timber.d("PAIR_FLOW: Peer saved locally — pairing complete for userId=${data.userId}")
                _effect.emit(QrPairUiEffect.Success("Paired with '$contactName' successfully!"))
            } catch (e: Exception) {
                Timber.e(e, "QR_FLOW: QR payload parse failed")
                _effect.emit(QrPairUiEffect.Error("Invalid pairing payload: ${e.message}"))
            }
        }
    }
}

sealed interface QrPairUiEffect {
    data class Success(val message: String) : QrPairUiEffect
    data class Error(val message: String) : QrPairUiEffect
}
