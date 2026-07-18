/*
 * Offline Emergency Mesh Communication System
 * Copyright (c) 2024. All rights reserved.
 */

package com.mesh.emergency.core.model

/**
 * Base interface representing a data layer database entity (e.g. Room Entity).
 */
interface BaseEntity {
    /**
     * Unique identifier for the data layer entity in the storage schema.
     */
    val entityId: String
}
