/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.di

import com.mesh.emergency.core.communication.CommunicationManager
import com.mesh.emergency.core.communication.PairingService
import com.mesh.emergency.core.communication.Transport
import com.mesh.emergency.core.messaging.MessagingService
import com.mesh.emergency.data.communication.CommunicationManagerImpl
import com.mesh.emergency.data.communication.LoRaTransportStub
import com.mesh.emergency.data.communication.bluetooth.BluetoothTransportImpl
import com.mesh.emergency.data.communication.nfc.NfcTransportStub
import com.mesh.emergency.data.communication.usb.UsbSerialTransportStub
import com.mesh.emergency.data.communication.wifi.WiFiDirectTransportStub
import com.mesh.emergency.data.messaging.MessagingServiceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Named
import javax.inject.Singleton

/**
 * Hilt DI module for the transport-independent communication architecture.
 *
 * Transport Registration Pattern:
 * Each [Transport] implementation is registered via `@Binds @IntoSet`, which instructs
 * Hilt to inject a `Set<Transport>` into [CommunicationManagerImpl].
 *
 * To add a new transport technology:
 * 1. Create a class implementing [Transport] (e.g., LoRa, Wi-Fi Direct, USB Serial)
 * 2. Add a `@Binds @IntoSet @Singleton` binding here.
 * 3. No changes needed to [CommunicationManagerImpl], [MessagingService], or the UI layer.
 *
 * Transport Selection Priority:
 * Determined by [com.mesh.emergency.core.communication.TransportPriority.of]:
 *   BLUETOOTH (1) > LORA (2) > WIFI_DIRECT (3) > NFC (4) > USB_SERIAL (5) > MOCK (99)
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class CommunicationModule {

    // ── Core Abstractions ─────────────────────────────────────────────────────

    /** Binds [CommunicationManagerImpl] to the [CommunicationManager] interface. */
    @Binds
    @Singleton
    abstract fun bindCommunicationManager(impl: CommunicationManagerImpl): CommunicationManager

    /**
     * Binds the transport-agnostic [MessagingServiceImpl] to [MessagingService].
     * All ViewModels that need to send/receive messages depend exclusively on this interface.
     */
    @Binds
    @Singleton
    abstract fun bindMessagingService(impl: MessagingServiceImpl): MessagingService

    /**
     * Binds [BluetoothTransportImpl] as the [PairingService] for QR handshake operations.
     * [QrPairViewModel] depends on [PairingService], not [BluetoothTransportImpl] directly.
     */
    @Binds
    @Singleton
    abstract fun bindPairingService(impl: BluetoothTransportImpl): PairingService

    // ── Named qualifier for direct BLE transport access in hardware layer ─────

    /**
     * Provides [BluetoothTransportImpl] as a named [Transport] binding for components that
     * specifically need the Bluetooth transport (e.g., [HardwareManagerImpl], [BleDeviceRepositoryImpl]).
     *
     * This prevents breaking the hardware discovery layer when future transports are added.
     */
    @Binds
    @Singleton
    @Named("bluetooth")
    abstract fun bindBluetoothTransportNamed(impl: BluetoothTransportImpl): Transport

    // ── Transport Set (Multi-bindings) ────────────────────────────────────────
    // Each @IntoSet binding adds the implementation to the Set<Transport> injected
    // into CommunicationManagerImpl. Priority is evaluated at runtime.

    /** Registers Bluetooth BLE transport. Active and primary transport (priority 1). */
    @Binds
    @IntoSet
    @Singleton
    abstract fun bindBluetoothTransport(impl: BluetoothTransportImpl): Transport

    /**
     * Registers LoRa transport stub. Reports UNAVAILABLE — implement when LoRa hardware is ready.
     * When [LoRaTransportStub] returns CONNECTED, it will automatically be selected over Bluetooth
     * if it has higher priority, or used as a fallback if Bluetooth is unavailable.
     */
    @Binds
    @IntoSet
    @Singleton
    abstract fun bindLoRaTransport(impl: LoRaTransportStub): Transport

    /** Registers Wi-Fi Direct transport stub. Reports UNAVAILABLE until implemented. */
    @Binds
    @IntoSet
    @Singleton
    abstract fun bindWifiDirectTransport(impl: WiFiDirectTransportStub): Transport

    /** Registers NFC transport stub. Reports UNAVAILABLE until implemented. */
    @Binds
    @IntoSet
    @Singleton
    abstract fun bindNfcTransport(impl: NfcTransportStub): Transport

    /** Registers USB Serial transport stub. Reports UNAVAILABLE until implemented. */
    @Binds
    @IntoSet
    @Singleton
    abstract fun bindUsbSerialTransport(impl: UsbSerialTransportStub): Transport
}
