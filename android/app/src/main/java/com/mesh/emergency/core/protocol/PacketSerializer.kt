/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.protocol

import com.mesh.emergency.data.local.entity.DbMessagePriority
import com.mesh.emergency.data.local.entity.DbMessageType
import org.json.JSONObject
import java.util.Base64
import java.util.zip.CRC32

/**
 * Serializer converting between [Packet] objects and standard JSON strings.
 * Uses standard java.util.Base64 to execute natively under standard JVM tests.
 */
object PacketSerializer {

    /** Encodes [Packet] instance into standard JSON format. */
    fun serializeToJson(packet: Packet): String {
        val json = JSONObject()
        val headerJson = JSONObject()
        headerJson.put("v", packet.header.version)
        headerJson.put("pid", packet.header.packetId)
        headerJson.put("sid", packet.header.senderId)
        headerJson.put("rid", packet.header.receiverId)
        headerJson.put("mt", packet.header.messageType.name)
        headerJson.put("pr", packet.header.priority.name)
        headerJson.put("ttl", packet.header.ttl)
        headerJson.put("hc", packet.header.hopCount)
        headerJson.put("ts", packet.header.timestamp)

        json.put("h", headerJson)
        json.put("p", Base64.getEncoder().encodeToString(packet.encryptedPayload))
        packet.authenticationTag?.let {
            json.put("tag", Base64.getEncoder().encodeToString(it))
        }
        json.put("crc", packet.checksum)
        return json.toString()
    }

    /** Decodes JSON string back into a [Packet] instance. */
    fun deserializeFromJson(jsonStr: String): Packet {
        val json = JSONObject(jsonStr)
        val headerJson = json.getJSONObject("h")

        val header = PacketHeader(
            version = headerJson.getInt("v"),
            packetId = headerJson.getString("pid"),
            senderId = headerJson.getString("sid"),
            receiverId = headerJson.getString("rid"),
            messageType = DbMessageType.valueOf(headerJson.getString("mt")),
            priority = DbMessagePriority.valueOf(headerJson.getString("pr")),
            ttl = headerJson.getLong("ttl"),
            hopCount = headerJson.getInt("hc"),
            timestamp = headerJson.getLong("ts")
        )

        val encryptedPayload = Base64.getDecoder().decode(json.getString("p"))
        val authenticationTag = if (json.has("tag")) {
            Base64.getDecoder().decode(json.getString("tag"))
        } else {
            null
        }
        val checksum = json.getLong("crc")

        return Packet(header, encryptedPayload, authenticationTag, checksum)
    }

    /** Computes CRC32 checksum over header elements and payload data. */
    fun calculateChecksum(header: PacketHeader, payload: ByteArray): Long {
        val crc = CRC32()
        val headerString = "${header.packetId}-${header.senderId}-${header.receiverId}-${header.messageType}-${header.priority}-${header.ttl}-${header.timestamp}"
        crc.update(headerString.toByteArray(Charsets.UTF_8))
        crc.update(payload)
        return crc.value
    }
}
