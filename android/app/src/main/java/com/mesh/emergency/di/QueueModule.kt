/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.di

import com.mesh.emergency.core.communication.forward.ForwardingEngine
import com.mesh.emergency.core.communication.queue.MessageQueueManager
import com.mesh.emergency.data.communication.forward.ForwardingEngineImpl
import com.mesh.emergency.data.communication.queue.MessageQueueManagerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module binding local Store & Forward queue manager interfaces.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class QueueModule {

    /** Binds [MessageQueueManagerImpl] to the [MessageQueueManager] interface. */
    @Binds
    @Singleton
    abstract fun bindMessageQueueManager(impl: MessageQueueManagerImpl): MessageQueueManager

    /** Binds [ForwardingEngineImpl] to the [ForwardingEngine] interface. */
    @Binds
    @Singleton
    abstract fun bindForwardingEngine(impl: ForwardingEngineImpl): ForwardingEngine
}
