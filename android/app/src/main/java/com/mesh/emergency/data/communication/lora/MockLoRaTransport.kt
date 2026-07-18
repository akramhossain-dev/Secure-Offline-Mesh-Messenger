/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.data.communication.lora

import com.mesh.emergency.core.common.result.Result
import com.mesh.emergency.core.communication.Transport
import com.mesh.emergency.core.communication.TransportStatus
import com.mesh.emergency.core.communication.TransportType
import com.mesh.emergency.core.communication.lora.LoRaSimulationManager
import com.mesh.emergency.core.communication.lora.SimulatedSignal
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Simulated LoRa transceiver implementation of [Transport] executing delay and loss rates.
 */
@Singleton
class MockLoRaTransport @Inject constructor(
    private val simulationManager: LoRaSimulationManager
) : Transport {

    override val type: TransportType = TransportType.LORA

    private val _status = MutableStateFlow(TransportStatus.DISCONNECTED)
    override val status: StateFlow<TransportStatus> = _status.asStateFlow()

    private val _receiveFlow = MutableSharedFlow<ByteArray>(extraBufferCapacity = 32)

    override suspend fun connect(): Result<Unit> {
        val config = simulationManager.config.value
        if (config.signalStrength == SimulatedSignal.DISCONNECTED) {
            _status.value = TransportStatus.UNAVAILABLE
            return Result.Error(Exception("LoRa network is out of range"))
        }

        _status.value = TransportStatus.CONNECTING
        delay(200L)
        _status.value = TransportStatus.CONNECTED
        return Result.Success(Unit)
    }

    override suspend fun disconnect(): Result<Unit> {
        _status.value = TransportStatus.DISCONNECTED
        return Result.Success(Unit)
    }

    override suspend fun send(data: ByteArray): Result<Unit> {
        if (_status.value != TransportStatus.CONNECTED) {
            return Result.Error(Exception("LoRa transport is not in connected state"))
        }

        val config = simulationManager.config.value

        // 1. Simulate transmission delay
        delay(config.delayLevel.ms)

        // 2. Simulate timeout failure
        if (config.simulateTimeout) {
            delay(3000L) // Block thread to simulate timeout
            return Result.Error(Exception("LoRa transmission timed out"))
        }

        // 3. Simulate random packet loss rolls
        val roll = Math.random().toFloat()
        if (roll < config.packetLossRate) {
            return Result.Error(Exception("Packet lost during LoRa propagation link"))
        }

        // 4. Echo simulation (emits back message to receiver after a small processing gap)
        val echoResponse = "[Echo-LoRa] received: ".toByteArray() + data
        _receiveFlow.tryEmit(echoResponse)

        return Result.Success(Unit)
    }

    override fun receive(): Flow<ByteArray> = _receiveFlow.asSharedFlow()
}
