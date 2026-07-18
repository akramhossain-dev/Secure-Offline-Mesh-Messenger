/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.communication.forward

/**
 * Interface contract managing forwarding processes and lifecycle sweeps.
 */
interface ForwardingEngine {
    /** Audits and forwards eligible enqueued message packets. */
    suspend fun processQueue()

    /** Scans and clean/expires stale messages past their TTL values. */
    suspend fun cleanExpiredMessages()
}
