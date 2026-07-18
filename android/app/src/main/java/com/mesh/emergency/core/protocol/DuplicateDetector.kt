/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.protocol

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thread-safe checker pruning duplicate incoming packet broadcasts.
 */
@Singleton
class DuplicateDetector @Inject constructor() {
    private val processedIds = mutableSetOf<String>()

    /**
     * Returns true if [packetId] has already been logged.
     */
    @Synchronized
    fun isDuplicate(packetId: String): Boolean {
        return if (processedIds.contains(packetId)) {
            true
        } else {
            processedIds.add(packetId)
            // Safety cap: prune earliest items when size exceeds 1000 items
            if (processedIds.size > 1000) {
                val iterator = processedIds.iterator()
                if (iterator.hasNext()) {
                    iterator.next()
                    iterator.remove()
                }
            }
            false
        }
    }

    /** Clears active logged list entries. */
    @Synchronized
    fun clear() {
        processedIds.clear()
    }
}
