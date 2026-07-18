/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.discovery

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Registry manager listing dynamic device discovery providers.
 */
@Singleton
class DiscoveryManager @Inject constructor() {
    private val providers = mutableListOf<DiscoveryProvider>()

    /** Register a discovery provider. */
    fun registerProvider(provider: DiscoveryProvider) {
        providers.add(provider)
    }

    /** Retrieve registered discovery providers. */
    fun getProviders(): List<DiscoveryProvider> = providers
}
