/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.discovery.qr

import org.json.JSONObject

/**
 * Serializer generating and parsing QR code handshake payload strings.
 */
object QRHandshakeManager {

    /** Converts [QRHandshakeData] properties to JSON formatted string payload. */
    fun generatePayload(data: QRHandshakeData): String {
        val json = JSONObject()
        json.put("v", data.version)
        json.put("did", data.deviceId)
        json.put("uid", data.userId)
        json.put("un", data.username)          // human-readable display name
        json.put("dt", data.deviceType)
        json.put("pub", data.publicKeyRef)
        json.put("ts", data.timestamp)
        json.put("ble", data.bleAddress)
        return json.toString()
    }

    /** Parses raw JSON payload string into a [QRHandshakeData] model. */
    fun parsePayload(payload: String): QRHandshakeData {
        val json = JSONObject(payload)
        return QRHandshakeData(
            version = json.getInt("v"),
            deviceId = json.getString("did"),
            userId = json.getString("uid"),
            username = json.optString("un", ""),   // backwards-compatible
            deviceType = json.getString("dt"),
            publicKeyRef = json.getString("pub"),
            timestamp = json.getLong("ts"),
            bleAddress = json.optString("ble", "")
        )
    }
}
